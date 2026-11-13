package de.jscoding.bacheckin.core.device

import kotlinx.coroutines.flow.Flow

data class ActivationSubmission(
    val activationCode: String,
    val localPin: String,
    val biometricPreferred: Boolean
)

sealed interface DeviceAuthFailure {
    data object MissingDeviceCredential : DeviceAuthFailure
    data object DeviceKeyMissing : DeviceAuthFailure
    data object DeviceRevoked : DeviceAuthFailure
    data object InvalidActivationCode : DeviceAuthFailure
    data object ActivationCodeExpired : DeviceAuthFailure
    data object ActivationCodeAlreadyUsed : DeviceAuthFailure
    data object DeviceKeyRejected : DeviceAuthFailure
    data object NetworkError : DeviceAuthFailure
    data class Unknown(val message: String) : DeviceAuthFailure
}

sealed interface DeviceAuthResult {
    data object Success : DeviceAuthResult
    data class Failure(val reason: DeviceAuthFailure) : DeviceAuthResult
}

interface DeviceAuthRepository {
    fun observeDeviceBinding(): Flow<DeviceBindingState>

    suspend fun validateActivationCode(activationCode: String): DeviceAuthResult

    suspend fun activateDevice(submission: ActivationSubmission): DeviceAuthResult

    suspend fun openDeviceSession(): DeviceAuthResult

    suspend fun revokeLocalDevice(): DeviceAuthResult
}
