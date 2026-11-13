package de.jscoding.bacheckin.feature.unlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.jscoding.bacheckin.core.device.DeviceAuthFailure
import de.jscoding.bacheckin.core.device.DeviceAuthResult
import de.jscoding.bacheckin.core.device.ObserveDeviceSecurityUseCase
import de.jscoding.bacheckin.core.device.OpenDeviceSessionUseCase
import de.jscoding.bacheckin.core.security.LocalPinStore
import de.jscoding.bacheckin.core.session.InactivityDetector
import de.jscoding.bacheckin.core.settings.ObserveAppSettingsUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UnlockUiState(
    val pin: String = "",
    val pinError: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface UnlockUiEvent {
    data object UnlockSucceeded : UnlockUiEvent
    data object DeviceRevoked : UnlockUiEvent
}

@HiltViewModel
class UnlockViewModel @Inject constructor(
    private val openDeviceSessionUseCase: OpenDeviceSessionUseCase,
    private val localPinStore: LocalPinStore,
    private val inactivityDetector: InactivityDetector,
    observeAppSettingsUseCase: ObserveAppSettingsUseCase,
    observeDeviceSecurityUseCase: ObserveDeviceSecurityUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(UnlockUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UnlockUiEvent>()
    val events = _events.asSharedFlow()

    val biometricPreferred = observeAppSettingsUseCase()
        .map { it.biometricPreferred }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    val canUseBiometricAuth = observeDeviceSecurityUseCase()
        .map { it.canUseBiometricAuth }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    fun onPinChange(newPin: String) {
        if (newPin.length > 5 || newPin.any { !it.isDigit() }) return

        _uiState.value = _uiState.value.copy(
            pin = newPin,
            pinError = false,
            errorMessage = null
        )

        if (newPin.length == 5) {
            unlockWithLocalPin(newPin)
        }
    }

    fun openSessionAfterLocalUnlock() {
        viewModelScope.launch {
            _uiState.value = UnlockUiState(isLoading = true)
            when (val result = openDeviceSessionUseCase()) {
                DeviceAuthResult.Success -> {
                    inactivityDetector.markInteraction()
                    _uiState.value = UnlockUiState()
                    _events.emit(UnlockUiEvent.UnlockSucceeded)
                }

                is DeviceAuthResult.Failure -> {
                    if (result.reason == DeviceAuthFailure.DeviceRevoked) {
                        _uiState.value = UnlockUiState()
                        _events.emit(UnlockUiEvent.DeviceRevoked)
                    } else {
                        _uiState.value = UnlockUiState(
                            isLoading = false,
                            errorMessage = result.reason.toMessage()
                        )
                    }
                }
            }
        }
    }

    private fun unlockWithLocalPin(pin: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            if (localPinStore.matches(pin)) {
                inactivityDetector.markInteraction()
                _uiState.value = UnlockUiState()
                _events.emit(UnlockUiEvent.UnlockSucceeded)
            } else {
                _uiState.value = UnlockUiState(
                    pin = "",
                    pinError = true,
                    isLoading = false,
                    errorMessage = "Die PIN ist falsch."
                )
            }
        }
    }

    fun showError(message: String) {
        _uiState.value = _uiState.value.copy(
            errorMessage = message,
            pinError = false
        )
    }

    private fun DeviceAuthFailure.toMessage(): String = when (this) {
        DeviceAuthFailure.MissingDeviceCredential -> "Gerätebindung fehlt."
        DeviceAuthFailure.DeviceKeyMissing -> "Geräteschlüssel fehlt. Neue Freischaltung erforderlich."
        DeviceAuthFailure.DeviceRevoked -> "Dieses Gerät wurde deaktiviert."
        DeviceAuthFailure.InvalidActivationCode -> "Der Freischaltungscode ist ungültig."
        DeviceAuthFailure.ActivationCodeExpired -> "Der Freischaltungscode ist abgelaufen."
        DeviceAuthFailure.ActivationCodeAlreadyUsed -> "Der Freischaltungscode wurde bereits verwendet."
        DeviceAuthFailure.DeviceKeyRejected -> "Geräteschlüssel konnte nicht verifiziert werden."
        DeviceAuthFailure.NetworkError -> "Netzwerkfehler. Bitte erneut versuchen."
        is DeviceAuthFailure.Unknown -> message
    }
}
