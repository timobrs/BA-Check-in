package de.jscoding.bacheckin.core.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed interface BiometricGateResult {
    data object Success : BiometricGateResult
    data object Cancelled : BiometricGateResult
    data object NotAvailable : BiometricGateResult
    data class Error(val message: String) : BiometricGateResult
}

class BiometricGate(
    private val activity: FragmentActivity
) {
    suspend fun authenticate(
        useBiometrics: Boolean = true,
        title: String = "App entsperren",
        subtitle: String = "PIN, Passwort oder Biometrie verwenden"
    ): BiometricGateResult {
        val allowedAuthenticators = if (useBiometrics) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }

        val canAuthenticate = BiometricManager.from(activity)
            .canAuthenticate(allowedAuthenticators)

        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            return BiometricGateResult.NotAvailable
        }

        return suspendCancellableCoroutine { continuation ->
            val prompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(
                        result: BiometricPrompt.AuthenticationResult
                    ) {
                        if (continuation.isActive) {
                            continuation.resume(BiometricGateResult.Success)
                        }
                    }

                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence
                    ) {
                        if (!continuation.isActive) return

                        val result = when (errorCode) {
                            BiometricPrompt.ERROR_USER_CANCELED,
                            BiometricPrompt.ERROR_CANCELED,
                            BiometricPrompt.ERROR_NEGATIVE_BUTTON -> BiometricGateResult.Cancelled
                            else -> BiometricGateResult.Error(errString.toString())
                        }
                        continuation.resume(result)
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(allowedAuthenticators)
                .build()

            prompt.authenticate(promptInfo)
            continuation.invokeOnCancellation { prompt.cancelAuthentication() }
        }
    }
}
