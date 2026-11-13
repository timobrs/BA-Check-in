package de.jscoding.bacheckin.feature.launch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.jscoding.bacheckin.core.device.DeviceBindingStatus
import de.jscoding.bacheckin.core.device.ObserveDeviceBindingUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface LaunchUiState {
    data object Loading : LaunchUiState
    data object NeedsActivation : LaunchUiState
    data object NeedsUnlock : LaunchUiState
    data object DeviceRevoked : LaunchUiState
}

@HiltViewModel
class LaunchViewModel @Inject constructor(
    observeDeviceBindingUseCase: ObserveDeviceBindingUseCase
) : ViewModel() {

    val uiState = observeDeviceBindingUseCase()
        .map { state ->
            when (state.status) {
                DeviceBindingStatus.UNACTIVATED -> LaunchUiState.NeedsActivation
                DeviceBindingStatus.ACTIVATED -> LaunchUiState.NeedsUnlock
                DeviceBindingStatus.REVOKED -> LaunchUiState.DeviceRevoked
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LaunchUiState.Loading
        )
}
