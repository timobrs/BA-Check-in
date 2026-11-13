package de.jscoding.bacheckin.feature.unlock

import androidx.activity.compose.LocalActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.jscoding.bacheckin.core.security.BiometricGate
import de.jscoding.bacheckin.core.security.BiometricGateResult
import kotlinx.coroutines.launch

@Composable
fun UnlockRoute(
    showAlreadyActivatedWarning: Boolean = false,
    onUnlockSucceeded: () -> Unit,
    onDeviceRevoked: () -> Unit,
    viewModel: UnlockViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val biometricPreferred by viewModel.biometricPreferred.collectAsStateWithLifecycle()
    val canUseBiometricAuth by viewModel.canUseBiometricAuth.collectAsStateWithLifecycle()
    val activity = LocalActivity.current as? FragmentActivity
    val scope = rememberCoroutineScope()
    var biometricAutoPrompted by remember { mutableStateOf(false) }
    var showWarningDialog by remember { mutableStateOf(showAlreadyActivatedWarning) }

    fun startBiometricUnlock() {
        if (!canUseBiometricAuth) {
            viewModel.showError("Keine sichere Gerätesperre verfügbar. Bitte Android-PIN oder Biometrie einrichten.")
            return
        }
        
        if (activity == null) {
            viewModel.showError("Biometrie/PIN kann in diesem Kontext nicht gestartet werden.")
            return
        }

        scope.launch {
            when (val result = BiometricGate(activity).authenticate(
                useBiometrics = biometricPreferred
            )) {
                BiometricGateResult.Success -> viewModel.openSessionAfterLocalUnlock()
                BiometricGateResult.Cancelled -> Unit
                BiometricGateResult.NotAvailable -> viewModel.showError(
                    "Keine sichere Gerätesperre verfügbar. Bitte Android-PIN oder Biometrie einrichten."
                )

                is BiometricGateResult.Error -> viewModel.showError(result.message)
            }
        }
    }

    LaunchedEffect(biometricPreferred, activity, showWarningDialog, canUseBiometricAuth) {
        if (biometricPreferred && activity != null && !biometricAutoPrompted && !showWarningDialog && canUseBiometricAuth) {
            biometricAutoPrompted = true
            startBiometricUnlock()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                UnlockUiEvent.UnlockSucceeded -> onUnlockSucceeded()
                UnlockUiEvent.DeviceRevoked -> onDeviceRevoked()
            }
        }
    }

    if (showWarningDialog) {
        AlertDialog(
            onDismissRequest = { showWarningDialog = false },
            title = { Text("Aktion nicht möglich") },
            text = { Text("Du bist bereits auf diesem Gerät angemeldet. Um einen neuen Freischaltungscode zu nutzen, musst du dich zuerst in den Einstellungen abmelden.") },
            confirmButton = {
                TextButton(onClick = { showWarningDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    UnlockScreen(
        uiState = uiState,
        modifier = Modifier,
        onUnlock = { startBiometricUnlock() },
        onPinChange = viewModel::onPinChange,
        canUseBiometricAuth = canUseBiometricAuth
    )
}
