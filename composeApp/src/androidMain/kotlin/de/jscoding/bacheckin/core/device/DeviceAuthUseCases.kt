package de.jscoding.bacheckin.core.device

class ObserveDeviceBindingUseCase(
    private val repository: DeviceAuthRepository
) {
    operator fun invoke() = repository.observeDeviceBinding()
}

class ValidateActivationCodeUseCase(
    private val repository: DeviceAuthRepository
) {
    suspend operator fun invoke(activationCode: String): DeviceAuthResult =
        repository.validateActivationCode(activationCode)
}

class ActivateDeviceUseCase(
    private val repository: DeviceAuthRepository
) {
    suspend operator fun invoke(submission: ActivationSubmission): DeviceAuthResult =
        repository.activateDevice(submission)
}

class OpenDeviceSessionUseCase(
    private val repository: DeviceAuthRepository
) {
    suspend operator fun invoke(): DeviceAuthResult = repository.openDeviceSession()
}

class RevokeLocalDeviceUseCase(
    private val repository: DeviceAuthRepository
) {
    suspend operator fun invoke(): DeviceAuthResult = repository.revokeLocalDevice()
}
