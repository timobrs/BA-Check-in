package de.jscoding.bacheckin.feature.launch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LaunchRoute(
    code: String? = null,
    onNeedsActivation: (code: String?) -> Unit,
    onNeedsUnlock: (showAlreadyActivatedWarning: Boolean) -> Unit,
    onDeviceRevoked: () -> Unit,
    viewModel: LaunchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState, code) {
        when (uiState) {
            LaunchUiState.Loading -> Unit
            LaunchUiState.NeedsActivation -> onNeedsActivation(code)
            LaunchUiState.NeedsUnlock -> onNeedsUnlock(code != null)
            LaunchUiState.DeviceRevoked -> onDeviceRevoked()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
