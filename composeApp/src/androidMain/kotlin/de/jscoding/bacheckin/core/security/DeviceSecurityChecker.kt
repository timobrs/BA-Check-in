package de.jscoding.bacheckin.core.security

import android.app.KeyguardManager
import android.content.Context
import androidx.biometric.BiometricManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("Unused")
@Singleton
class DeviceSecurityChecker @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    
    fun hasSecureLockScreen(): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return keyguardManager.isDeviceSecure
    }

    fun hasBiometricHardware(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> false
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> false
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> false
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> false
            else -> false
        }
    }
    
    fun hasDeviceCredentials(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL) == 
                BiometricManager.BIOMETRIC_SUCCESS
    }

    fun canUseBiometricAuthentication(): Boolean {
        if (!hasSecureLockScreen()) return false
        
        val biometricManager = BiometricManager.from(context)
        val allowedAuthenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or 
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        
        return biometricManager.canAuthenticate(allowedAuthenticators) == 
                BiometricManager.BIOMETRIC_SUCCESS
    }

    fun canCreateKeysWithUserAuth(): Boolean = hasSecureLockScreen()
}
