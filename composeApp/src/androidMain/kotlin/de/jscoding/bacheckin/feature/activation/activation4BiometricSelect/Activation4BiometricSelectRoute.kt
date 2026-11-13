package de.jscoding.bacheckin.feature.activation.activation4BiometricSelect

import androidx.activity.compose.LocalActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.jscoding.bacheckin.core.security.BiometricGate
import de.jscoding.bacheckin.core.security.BiometricGateResult
import de.jscoding.bacheckin.feature.activation.ActivationEvent
import de.jscoding.bacheckin.feature.activation.ActivationViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun Activation4BiometricSelectRoute(
    viewModel: ActivationViewModel,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val activity = LocalActivity.current as? FragmentActivity
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                ActivationEvent.ActivationSucceeded -> onSubmit()
                ActivationEvent.ActivationCancelled -> onCancel()
                is ActivationEvent.ShowSnackbar -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(event.message)
                }
                else -> Unit
            }
        }
    }

    Activation4BiometricSelectScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBiometricPreferredChange = { enabled ->
            if (!enabled) {
                viewModel.onBiometricPreferredChange(false)
                return@Activation4BiometricSelectScreen
            }

            if (!uiState.canUseBiometricAuth) {
                viewModel.onBiometricPreferredChange(false)
                return@Activation4BiometricSelectScreen
            }

            if (activity == null) {
                viewModel.onBiometricPreferredChange(false)
                return@Activation4BiometricSelectScreen
            }

            scope.launch {
                when (BiometricGate(activity).authenticate()) {
                    BiometricGateResult.Success -> viewModel.onBiometricPreferredChange(true)
                    BiometricGateResult.Cancelled -> viewModel.onBiometricPreferredChange(false)
                    BiometricGateResult.NotAvailable -> viewModel.onBiometricPreferredChange(false)
                    is BiometricGateResult.Error -> viewModel.onBiometricPreferredChange(false)
                }
            }
        },
        onSubmit = viewModel::submitActivation,
        onBack = onBack,
        onCancel = viewModel::cancelActivation
    )
}
