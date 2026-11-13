package de.jscoding.bacheckin.core.device

import de.jscoding.bacheckin.core.security.DeviceSecurityChecker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ObserveDeviceSecurityUseCase @Inject constructor(
    private val deviceSecurityChecker: DeviceSecurityChecker
) {
    
    operator fun invoke(): Flow<DeviceSecurityState> = flow {
        emit(
            DeviceSecurityState(
                hasSecureLockScreen = deviceSecurityChecker.hasSecureLockScreen(),
                canUseBiometricAuth = deviceSecurityChecker.canUseBiometricAuthentication()
            )
        )
    }
    
    fun getCurrent(): DeviceSecurityState = DeviceSecurityState(
        hasSecureLockScreen = deviceSecurityChecker.hasSecureLockScreen(),
        canUseBiometricAuth = deviceSecurityChecker.canUseBiometricAuthentication()
    )
}

data class DeviceSecurityState(
    val hasSecureLockScreen: Boolean,
    val canUseBiometricAuth: Boolean
)
