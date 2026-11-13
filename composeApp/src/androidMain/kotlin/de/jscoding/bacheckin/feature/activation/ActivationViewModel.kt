package de.jscoding.bacheckin.feature.activation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.jscoding.bacheckin.core.device.ActivateDeviceUseCase
import de.jscoding.bacheckin.core.device.ActivationSubmission
import de.jscoding.bacheckin.core.device.DeviceAuthFailure
import de.jscoding.bacheckin.core.device.DeviceAuthResult
import de.jscoding.bacheckin.core.device.ObserveDeviceSecurityUseCase
import de.jscoding.bacheckin.core.device.ValidateActivationCodeUseCase
import de.jscoding.bacheckin.core.settings.SetBiometricPreferenceUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

data class ActivationUiState(
    val activationCode: String = "",
    val activationCodeError: String? = null,
    val pin: String = "",
    val pinError: Boolean = false,
    val confirmationPin: String = "",
    val confirmationPinError: Boolean = false,
    val biometricPreferred: Boolean = false,
    val canUseBiometricAuth: Boolean = false,
    val isLoading: Boolean = false
) {
    fun isActivationCodeValid(code: String = activationCode): Boolean =
        code.trim().replace("-", "").length >= 8

    fun isPinValid(pin: String = this.pin): Boolean =
        pin.length == 5 && pin.all { it.isDigit() }

    fun isConfirmationPinValid(): Boolean =
        pin == confirmationPin && isPinValid(pin)
}

sealed interface ActivationEvent {
    data class ShowSnackbar(val message: String) : ActivationEvent
    data object CodeAccepted : ActivationEvent
    data object PinSelected : ActivationEvent
    data object PinConfirmed : ActivationEvent
    data object ActivationSucceeded : ActivationEvent
    data object ActivationCancelled : ActivationEvent
}

@HiltViewModel
class ActivationViewModel @Inject constructor(
    private val validateActivationCodeUseCase: ValidateActivationCodeUseCase,
    private val activateDeviceUseCase: ActivateDeviceUseCase,
    private val setBiometricPreferenceUseCase: SetBiometricPreferenceUseCase,
    observeDeviceSecurityUseCase: ObserveDeviceSecurityUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ActivationUiState(
            canUseBiometricAuth = observeDeviceSecurityUseCase.getCurrent().canUseBiometricAuth
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ActivationEvent>()
    val events = _events.asSharedFlow()

    fun onActivationCodeChange(newCode: String) {
        if (newCode.length > 32) return
        _uiState.value = _uiState.value.copy(
            activationCode = newCode.uppercase(),
            activationCodeError = null
        )
    }

    fun onPinChange(newPin: String) {
        clearPinErrors()
        if (newPin.length > 5 || newPin.any { !it.isDigit() }) return
        _uiState.value = _uiState.value.copy(pin = newPin)

        if (newPin.length == 5) {
            validatePinSelection()
        }
    }

    fun onConfirmationPinChange(newPin: String) {
        clearPinErrors()
        if (newPin.length > 5 || newPin.any { !it.isDigit() }) return
        _uiState.value = _uiState.value.copy(confirmationPin = newPin)

        if (newPin.length == 5) {
            validatePinConfirmation()
        }
    }

    fun onBiometricPreferredChange(enabled: Boolean) {
        val current = _uiState.value
        val canEnable = enabled && current.canUseBiometricAuth
        _uiState.value = current.copy(biometricPreferred = canEnable)
    }

    fun validateActivationCode() {
        val current = _uiState.value
        if (!current.isActivationCodeValid()) {
            _uiState.value = current.copy(
                activationCodeError = "Der Freischaltungscode ist zu kurz."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = current.copy(isLoading = true, activationCodeError = null)
            when (val result = validateActivationCodeUseCase(current.activationCode)) {
                DeviceAuthResult.Success -> {
                    _uiState.value = current.copy(isLoading = false, activationCodeError = null)
                    _events.emit(ActivationEvent.CodeAccepted)
                }

                is DeviceAuthResult.Failure -> {
                    _uiState.value = current.copy(
                        isLoading = false,
                        activationCodeError = result.reason.toMessage()
                    )
                }
            }
        }
    }

    fun validatePinSelection() {
        val current = _uiState.value
        viewModelScope.launch {
            _uiState.value = current.copy(isLoading = true)
            delay(250.milliseconds)
            if (current.isPinValid()) {
                _events.emit(ActivationEvent.PinSelected)
                _uiState.value = current.copy(isLoading = false)
            } else {
                _uiState.value = current.copy(isLoading = false, pinError = true)
                _events.emit(ActivationEvent.ShowSnackbar("Die PIN muss aus 5 Ziffern bestehen."))
            }
        }
    }

    fun validatePinConfirmation() {
        val current = _uiState.value
        viewModelScope.launch {
            _uiState.value = current.copy(isLoading = true)
            delay(250.milliseconds)
            if (current.isConfirmationPinValid()) {
                _events.emit(ActivationEvent.PinConfirmed)
                _uiState.value = current.copy(isLoading = false)
            } else {
                _uiState.value = current.copy(
                    isLoading = false,
                    confirmationPin = "",
                    confirmationPinError = true
                )
                _events.emit(ActivationEvent.ShowSnackbar("Die PINs stimmen nicht überein."))
            }
        }
    }

    fun submitActivation() {
        val current = _uiState.value
        if (!current.isActivationCodeValid()) {
            _uiState.value = current.copy(
                activationCodeError = "Der Freischaltungscode ist zu kurz."
            )
            return
        }
        if (!current.isConfirmationPinValid()) {
            _uiState.value = current.copy(confirmationPinError = true)
            return
        }

        viewModelScope.launch {
            _uiState.value = current.copy(isLoading = true)

            when (
                val result = activateDeviceUseCase(
                    ActivationSubmission(
                        activationCode = current.activationCode.trim(),
                        localPin = current.pin,
                        biometricPreferred = current.biometricPreferred && current.canUseBiometricAuth
                    )
                )
            ) {
                DeviceAuthResult.Success -> {
                    setBiometricPreferenceUseCase(current.biometricPreferred)
                    _uiState.value = ActivationUiState()
                    _events.emit(ActivationEvent.ActivationSucceeded)
                }

                is DeviceAuthResult.Failure -> {
                    _uiState.value = current.copy(isLoading = false)
                    _events.emit(ActivationEvent.ShowSnackbar(result.reason.toMessage()))
                }
            }
        }
    }

    fun cancelActivation() {
        _uiState.value = ActivationUiState()
        viewModelScope.launch { _events.emit(ActivationEvent.ActivationCancelled) }
    }

    fun resetPinSelection() {
        _uiState.value = _uiState.value.copy(
            pin = "",
            confirmationPin = "",
            pinError = false,
            confirmationPinError = false
        )
    }

    private fun clearPinErrors() {
        _uiState.value = _uiState.value.copy(
            pinError = false,
            confirmationPinError = false
        )
    }

    private fun DeviceAuthFailure.toMessage(): String = when (this) {
        DeviceAuthFailure.MissingDeviceCredential -> "Gerätebindung fehlt."
        DeviceAuthFailure.DeviceKeyMissing -> "Der Geräteschlüssel fehlt. Neue Freischaltung erforderlich."
        DeviceAuthFailure.DeviceRevoked -> "Dieses Gerät wurde deaktiviert."
        DeviceAuthFailure.InvalidActivationCode -> "Der Freischaltungscode ist ungültig."
        DeviceAuthFailure.ActivationCodeExpired -> "Der Freischaltungscode ist abgelaufen."
        DeviceAuthFailure.ActivationCodeAlreadyUsed -> "Der Freischaltungscode wurde bereits verwendet."
        DeviceAuthFailure.DeviceKeyRejected -> "Geräteschlüssel konnte nicht verifiziert werden."
        DeviceAuthFailure.NetworkError -> "Netzwerkfehler. Bitte erneut versuchen."
        is DeviceAuthFailure.Unknown -> message
    }
}
