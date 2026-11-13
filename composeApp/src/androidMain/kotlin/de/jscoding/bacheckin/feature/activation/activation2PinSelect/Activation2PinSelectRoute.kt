package de.jscoding.bacheckin.feature.activation.activation2PinSelect

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.jscoding.bacheckin.feature.activation.ActivationEvent
import de.jscoding.bacheckin.feature.activation.ActivationViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun Activation2PinSelectRoute(
    viewModel: ActivationViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                ActivationEvent.PinSelected -> onNext()
                ActivationEvent.ActivationCancelled -> onCancel()
                is ActivationEvent.ShowSnackbar -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(event.message)
                }
                else -> Unit
            }
        }
    }

    Activation2PinSelectScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onPinChange = viewModel::onPinChange,
        onBack = {
            viewModel.resetPinSelection()
            onBack()
        },
        onCancel = viewModel::cancelActivation
    )
}
