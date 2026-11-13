package de.jscoding.bacheckin.feature.revoked

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.jscoding.bacheckin.core.device.RevokeLocalDeviceUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RevokedDeviceUiState(
    val isResetting: Boolean = false
)

sealed interface RevokedDeviceUiEvent {
    data object ResetFinished : RevokedDeviceUiEvent
}

@HiltViewModel
class RevokedDeviceViewModel @Inject constructor(
    private val revokeLocalDeviceUseCase: RevokeLocalDeviceUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(RevokedDeviceUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<RevokedDeviceUiEvent>()
    val events = _events.asSharedFlow()

    fun clearLocalDevice() {
        viewModelScope.launch {
            _uiState.value = RevokedDeviceUiState(isResetting = true)
            revokeLocalDeviceUseCase()
            _uiState.value = RevokedDeviceUiState(isResetting = false)
            _events.emit(RevokedDeviceUiEvent.ResetFinished)
        }
    }
}
