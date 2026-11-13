package de.jscoding.bacheckin.core.session

import android.util.Base64
import de.jscoding.bacheckin.core.device.AndroidDeviceStateStore
import de.jscoding.bacheckin.core.security.DeviceKeyManager
import de.jscoding.bacheckin.shared.dataSource.BackendRemoteDataSourceImpl
import de.jscoding.bacheckin.shared.model.BackendDeviceErrorCode
import de.jscoding.bacheckin.shared.model.BackendException
import de.jscoding.bacheckin.shared.model.BackendRequests
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/** Fasst gleichzeitige Token-Refresh-Anfragen zusammen. */
@Singleton
class TokenManager @Inject constructor(
    private val secureTokenStore: SecureTokenStore,
    private val deviceStateStore: AndroidDeviceStateStore,
    private val backendRemoteDataSource: BackendRemoteDataSourceImpl,
    private val deviceKeyManager: DeviceKeyManager
) {
    companion object {
        private const val REFRESH_BUFFER_MS = 30_000L
    }

    private val _expiry = MutableStateFlow(secureTokenStore.read()?.accessTokenExpiresAtEpochMillis)
    val expiry: StateFlow<Long?> = _expiry.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch {
            deviceStateStore.state.collect { state ->
                state.accessTokenExpiresAtEpochMillis?.let { _expiry.value = it }
            }
        }
    }

    suspend fun getValidAccessToken(): String {
        val stored = secureTokenStore.read() ?: throw BackendException(
            code = BackendDeviceErrorCode.DEVICE_NOT_ACTIVE,
            message = "Kein gespeichertes Token"
        )

        val now = System.currentTimeMillis()
        if (now >= stored.accessTokenExpiresAtEpochMillis - REFRESH_BUFFER_MS) {
            refreshInternal()
        }

        return secureTokenStore.read()?.accessToken
            ?: throw BackendException(
                code = BackendDeviceErrorCode.DEVICE_NOT_ACTIVE,
                message = "Kein AccessToken verfügbar"
            )
    }

    suspend fun refreshNow(): Boolean {
        refreshInternal()
        return true
    }

    private suspend fun refreshInternal() = coroutineScope {
        val refreshJob = synchronized(lock) {
            currentRefreshJob ?: async(Dispatchers.IO) {
                performRefresh()
            }.also {
                currentRefreshJob = it
            }
        }

        try {
            return@coroutineScope refreshJob.await()
        } finally {
            synchronized(lock) {
                if (currentRefreshJob === refreshJob) {
                    currentRefreshJob = null
                }
            }
        }
    }

    private val lock = Any()
    private var currentRefreshJob: Deferred<Boolean>? = null

    private suspend fun performRefresh(): Boolean {
        val stored = secureTokenStore.read() ?: throw BackendException(
            code = BackendDeviceErrorCode.DEVICE_NOT_ACTIVE,
            message = "Kein gespeichertes Token"
        )

        val deviceCredentialId = deviceStateStore.state.first().deviceCredentialId
            ?: throw BackendException(
                code = BackendDeviceErrorCode.DEVICE_NOT_ACTIVE,
                message = "Keine Gerätekennung"
            )

        val refreshToken = stored.refreshToken ?: throw BackendException(
            code = BackendDeviceErrorCode.UNKNOWN,
            message = "Kein Refresh-Token vorhanden"
        )

        val timestamp = System.currentTimeMillis()
        val refreshHash = MessageDigest.getInstance("SHA-256").digest(refreshToken.toByteArray())
        val payload = listOf(
            "BACHECKIN_DEVICE_REFRESH_V1",
            deviceCredentialId,
            Base64.encodeToString(refreshHash, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING),
            timestamp.toString()
        ).joinToString("\n")
        val signature = deviceKeyManager.signPayloadBase64(payload.toByteArray())

        val response = runCatching {
            backendRemoteDataSource.refresh(
                BackendRequests.Refresh(deviceCredentialId, refreshToken, timestamp, signature)
            )
        }.getOrElse { error ->
            handleRefreshFailure(error)
            throw error
        }

        secureTokenStore.save(
            StoredTokens(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken ?: refreshToken,
                accessTokenExpiresAtEpochMillis = response.accessTokenExpiresAtEpochMillis
            )
        )
        deviceStateStore.updateTokenExpiry(response.accessTokenExpiresAtEpochMillis)
        _expiry.update { response.accessTokenExpiresAtEpochMillis }
        return true
    }

    private suspend fun handleRefreshFailure(error: Throwable) {
        val backendException = error as? BackendException ?: return
        when (backendException.code) {
            BackendDeviceErrorCode.DEVICE_REVOKED,
            BackendDeviceErrorCode.DEVICE_NOT_ACTIVE,
            BackendDeviceErrorCode.DEVICE_REPLACED -> {
                secureTokenStore.clear()
                deviceStateStore.setRevoked()
                _expiry.value = null
            }

            else -> Unit
        }
    }
}
