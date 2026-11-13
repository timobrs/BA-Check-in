package de.jscoding.bacheckin.feature.revoked

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun RevokedDeviceRoute(
    onResetFinished: () -> Unit,
    viewModel: RevokedDeviceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                RevokedDeviceUiEvent.ResetFinished -> onResetFinished()
            }
        }
    }

    RevokedDeviceScreen(
        uiState = uiState,
        onClearLocalDevice = viewModel::clearLocalDevice
    )
}
