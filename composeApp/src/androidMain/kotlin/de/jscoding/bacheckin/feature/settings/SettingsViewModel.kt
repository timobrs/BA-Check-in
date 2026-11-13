package de.jscoding.bacheckin.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.jscoding.bacheckin.core.device.AppVersionProvider
import de.jscoding.bacheckin.core.device.DeviceAuthResult
import de.jscoding.bacheckin.core.device.ObserveDeviceBindingUseCase
import de.jscoding.bacheckin.core.device.ObserveDeviceSecurityUseCase
import de.jscoding.bacheckin.core.device.RevokeLocalDeviceUseCase
import de.jscoding.bacheckin.shared.model.UserRole
import de.jscoding.bacheckin.core.settings.AppSettings
import de.jscoding.bacheckin.core.settings.ObserveAppSettingsUseCase
import de.jscoding.bacheckin.core.settings.SetBiometricPreferenceUseCase
import de.jscoding.bacheckin.core.settings.SetThemePreferenceUseCase
import de.jscoding.bacheckin.core.settings.ThemePreference
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val appSettings: AppSettings = AppSettings(),
    val userRole: UserRole = UserRole.NONE,
    val isRemovingDevice: Boolean = false,
    val canUseBiometricAuth: Boolean = false
)

sealed interface SettingsUiEvent {
    data object DeviceRemoved : SettingsUiEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    appVersionProvider: AppVersionProvider,
    observeAppSettingsUseCase: ObserveAppSettingsUseCase,
    observeDeviceBindingUseCase: ObserveDeviceBindingUseCase,
    observeDeviceSecurityUseCase: ObserveDeviceSecurityUseCase,
    private val setThemePreferenceUseCase: SetThemePreferenceUseCase,
    private val setBiometricPreferenceUseCase: SetBiometricPreferenceUseCase,
    private val revokeLocalDeviceUseCase: RevokeLocalDeviceUseCase
) : ViewModel() {

    val appVersion: String = appVersionProvider.versionName

    val uiState = combine(
        observeAppSettingsUseCase(),
        observeDeviceBindingUseCase(),
        observeDeviceSecurityUseCase()
    ) { appSettings, deviceBinding, deviceSecurityState ->
        SettingsUiState(
            appSettings = appSettings,
            userRole = deviceBinding.userRole,
            canUseBiometricAuth = deviceSecurityState.canUseBiometricAuth
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    private val _events = MutableSharedFlow<SettingsUiEvent>()
    val events = _events.asSharedFlow()

    fun setThemePreference(themePreference: ThemePreference) {
        viewModelScope.launch {
            setThemePreferenceUseCase(themePreference)
        }
    }

    fun setBiometricPreferred(enabled: Boolean) {
        viewModelScope.launch {
            setBiometricPreferenceUseCase(enabled)
        }
    }

    fun removeDeviceBinding() {
        viewModelScope.launch {
            if (revokeLocalDeviceUseCase() is DeviceAuthResult.Success) {
                _events.emit(SettingsUiEvent.DeviceRemoved)
            }
        }
    }
}
