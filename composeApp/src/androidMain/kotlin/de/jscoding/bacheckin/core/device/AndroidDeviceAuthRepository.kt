package de.jscoding.bacheckin.core.device

import android.os.Build
import android.util.Base64
import de.jscoding.bacheckin.core.security.DeviceKeyManager
import de.jscoding.bacheckin.core.security.LocalPinStore
import de.jscoding.bacheckin.core.session.SecureTokenStore
import de.jscoding.bacheckin.core.session.StoredTokens
import de.jscoding.bacheckin.shared.model.BackendDeviceErrorCode
import de.jscoding.bacheckin.shared.model.BackendDtos
import de.jscoding.bacheckin.shared.model.BackendException
import de.jscoding.bacheckin.shared.model.BackendRequests
import de.jscoding.bacheckin.shared.model.BackendRemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import java.security.MessageDigest

@Singleton
class AndroidDeviceAuthRepository @Inject constructor(
    private val backendRemoteDataSource: BackendRemoteDataSource,
    private val deviceStateStore: AndroidDeviceStateStore,
    private val deviceKeyManager: DeviceKeyManager,
    private val localPinStore: LocalPinStore,
    private val secureTokenStore: SecureTokenStore,
    private val appVersionProvider: AppVersionProvider
) : DeviceAuthRepository {

    override fun observeDeviceBinding(): Flow<DeviceBindingState> = deviceStateStore.state

    override suspend fun validateActivationCode(activationCode: String): DeviceAuthResult {
        return runCatching<DeviceAuthResult> {
            backendRemoteDataSource.validateActivationCode(activationCode.trim())
            DeviceAuthResult.Success
        }.getOrElse { error ->
            DeviceAuthResult.Failure(error.toDeviceAuthFailure())
        }
    }

    override suspend fun activateDevice(
        submission: ActivationSubmission
    ): DeviceAuthResult {
        return runCatching<DeviceAuthResult> {
            backendRemoteDataSource.validateActivationCode(submission.activationCode.trim())
            val challenge = backendRemoteDataSource.createActivationChallenge()
            Base64.decode(
                challenge.challengeBase64,
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )

            val keyMaterial = deviceKeyManager.generateDeviceKey(preferStrongBox = true)

            val publicKeyFingerprint = MessageDigest.getInstance("SHA-256").digest(
                Base64.decode(keyMaterial.publicKeyBase64, Base64.DEFAULT)
            )
            val activationProof = listOf(
                "BACHECKIN_DEVICE_ACTIVATION_V1",
                challenge.activationSessionId,
                challenge.challengeBase64.trimEnd('='),
                Base64.encodeToString(
                    publicKeyFingerprint,
                    Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
                )
            ).joinToString("\n")
            val activationSignature = deviceKeyManager.signPayloadBase64(
                activationProof.toByteArray(Charsets.UTF_8)
            )

            val activationResponse = backendRemoteDataSource.completeActivation(
                BackendRequests.CompleteActivation(
                    activationSessionId = challenge.activationSessionId,
                    activationCode = submission.activationCode,
                    publicKeyBase64 = keyMaterial.publicKeyBase64,
                    challengeSignatureBase64 = activationSignature,
                    deviceInfo = BackendDtos.DeviceInfo(
                        appVersionName = appVersionProvider.versionName,
                        appVersionCode = appVersionProvider.versionCode,
                        androidSdk = Build.VERSION.SDK_INT,
                        manufacturer = Build.MANUFACTURER,
                        model = Build.MODEL
                    )
                )
            )

            val authChallenge = backendRemoteDataSource.createAuthChallenge(
                BackendRequests.AuthChallenge(activationResponse.deviceCredentialId)
            )
            val authTimestamp = System.currentTimeMillis()
            val authSignature = deviceKeyManager.signPayloadBase64(
                buildCanonicalAuthPayload(
                    activationResponse.deviceCredentialId,
                    authChallenge.authSessionId,
                    authChallenge.challengeBase64,
                    authTimestamp
                ).toByteArray(Charsets.UTF_8)
            )
            val sessionResponse = backendRemoteDataSource.openDeviceSession(
                BackendRequests.DeviceSession(
                    activationResponse.deviceCredentialId,
                    authChallenge.authSessionId,
                    authTimestamp,
                    authSignature
                )
            )

            secureTokenStore.save(
                StoredTokens(
                    accessToken = sessionResponse.accessToken,
                    refreshToken = sessionResponse.refreshToken,
                    accessTokenExpiresAtEpochMillis = sessionResponse.accessTokenExpiresAtEpochMillis
                )
            )

            localPinStore.save(submission.localPin)

            deviceStateStore.setActivated(
                userId = activationResponse.userId,
                userRole = activationResponse.userRole,
                displayName = activationResponse.displayName,
                deviceCredentialId = activationResponse.deviceCredentialId,
                accessTokenExpiresAtEpochMillis = sessionResponse.accessTokenExpiresAtEpochMillis
            )

            DeviceAuthResult.Success
        }.getOrElse { error ->
            deviceKeyManager.deleteDeviceKey()
            secureTokenStore.clear()
            localPinStore.clear()
            DeviceAuthResult.Failure(error.toDeviceAuthFailure())
        }
    }

    override suspend fun openDeviceSession(): DeviceAuthResult {
        val binding = deviceStateStore.state.first()
        val deviceCredentialId = binding.deviceCredentialId
            ?: return DeviceAuthResult.Failure(DeviceAuthFailure.MissingDeviceCredential)

        if (!deviceKeyManager.hasDeviceKey()) {
            secureTokenStore.clear()
            localPinStore.clear()
            deviceStateStore.clear()
            return DeviceAuthResult.Failure(DeviceAuthFailure.DeviceKeyMissing)
        }

        return runCatching<DeviceAuthResult> {
            val challenge = backendRemoteDataSource.createAuthChallenge(
                BackendRequests.AuthChallenge(deviceCredentialId)
            )
            val timestamp = System.currentTimeMillis()
            val payload = buildCanonicalAuthPayload(
                deviceCredentialId = deviceCredentialId,
                authSessionId = challenge.authSessionId,
                challengeBase64 = challenge.challengeBase64,
                timestampEpochMillis = timestamp
            )

            val signatureBase64 = deviceKeyManager.signPayloadBase64(
                payload.toByteArray(Charsets.UTF_8)
            )

            val sessionResponse = backendRemoteDataSource.openDeviceSession(
                BackendRequests.DeviceSession(
                    deviceCredentialId = deviceCredentialId,
                    authSessionId = challenge.authSessionId,
                    timestampEpochMillis = timestamp,
                    signatureBase64 = signatureBase64
                )
            )

            secureTokenStore.save(
                StoredTokens(
                    accessToken = sessionResponse.accessToken,
                    refreshToken = sessionResponse.refreshToken
                        ?: secureTokenStore.read()?.refreshToken,
                    accessTokenExpiresAtEpochMillis =
                        sessionResponse.accessTokenExpiresAtEpochMillis
                )
            )
            deviceStateStore.updateTokenExpiry(
                sessionResponse.accessTokenExpiresAtEpochMillis
            )

            DeviceAuthResult.Success
        }.getOrElse { error ->
            val failure = error.toDeviceAuthFailure()
            if (failure == DeviceAuthFailure.DeviceRevoked) {
                secureTokenStore.clear()
                localPinStore.clear()
                deviceStateStore.setRevoked()
            }
            DeviceAuthResult.Failure(failure)
        }
    }

    override suspend fun revokeLocalDevice(): DeviceAuthResult {
        val deviceCredentialId = deviceStateStore.state.first().deviceCredentialId
        return runCatching {
            if (!deviceCredentialId.isNullOrBlank()) backendRemoteDataSource.unlinkCurrentDevice()
            secureTokenStore.clear()
            deviceKeyManager.deleteDeviceKey()
            localPinStore.clear()
            deviceStateStore.clear()
            DeviceAuthResult.Success
        }.getOrElse { error ->
            DeviceAuthResult.Failure(error.toDeviceAuthFailure())
        }
    }

    private fun buildCanonicalAuthPayload(
        deviceCredentialId: String,
        authSessionId: String,
        challengeBase64: String,
        timestampEpochMillis: Long
    ): String = listOf(
        "BACHECKIN_DEVICE_SESSION_V1",
        deviceCredentialId,
        authSessionId,
        challengeBase64,
        timestampEpochMillis.toString()
    ).joinToString(separator = "\n")

    private fun Throwable.toDeviceAuthFailure(): DeviceAuthFailure {
        val backendException = this as? BackendException
            ?: return DeviceAuthFailure.Unknown(message ?: "Unbekannter Fehler")

        return when (backendException.code) {
            BackendDeviceErrorCode.INVALID_ACTIVATION_CODE ->
                DeviceAuthFailure.InvalidActivationCode
            BackendDeviceErrorCode.ACTIVATION_CODE_EXPIRED ->
                DeviceAuthFailure.ActivationCodeExpired
            BackendDeviceErrorCode.ACTIVATION_CODE_ALREADY_USED ->
                DeviceAuthFailure.ActivationCodeAlreadyUsed
            BackendDeviceErrorCode.DEVICE_REVOKED,
            BackendDeviceErrorCode.DEVICE_NOT_ACTIVE,
            BackendDeviceErrorCode.DEVICE_REPLACED ->
                DeviceAuthFailure.DeviceRevoked
            BackendDeviceErrorCode.DEVICE_KEY_REJECTED ->
                DeviceAuthFailure.DeviceKeyRejected
            BackendDeviceErrorCode.NETWORK_ERROR,
            BackendDeviceErrorCode.RATE_LIMITED ->
                DeviceAuthFailure.NetworkError
            BackendDeviceErrorCode.UNKNOWN ->
                DeviceAuthFailure.Unknown(backendException.message ?: "Unbekannter Backendfehler")
        }
    }
}
