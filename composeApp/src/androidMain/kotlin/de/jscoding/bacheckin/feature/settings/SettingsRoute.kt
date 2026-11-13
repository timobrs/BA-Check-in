package de.jscoding.bacheckin.feature.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.jscoding.bacheckin.core.security.BiometricGate
import de.jscoding.bacheckin.core.security.BiometricGateResult
import kotlinx.coroutines.launch

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onDeviceRemoved: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current as? FragmentActivity
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SettingsUiEvent.DeviceRemoved -> onDeviceRemoved()
            }
        }
    }

    SettingsScreen(
        uiState = uiState,
        appVersion = viewModel.appVersion,
        onBack = onBack,
        onThemePreferenceSelected = viewModel::setThemePreference,
        onBiometricPreferredChanged = { enabled ->
            if (!enabled) {
                viewModel.setBiometricPreferred(false)
            } else if (activity == null) {
                viewModel.setBiometricPreferred(false)
            } else {
                scope.launch {
                    when (BiometricGate(activity).authenticate()) {
                        BiometricGateResult.Success -> viewModel.setBiometricPreferred(true)
                        BiometricGateResult.Cancelled -> viewModel.setBiometricPreferred(false)
                        BiometricGateResult.NotAvailable -> viewModel.setBiometricPreferred(false)
                        is BiometricGateResult.Error -> viewModel.setBiometricPreferred(false)
                    }
                }
            }
        },
        onRemoveDeviceBinding = viewModel::removeDeviceBinding
    )
}
