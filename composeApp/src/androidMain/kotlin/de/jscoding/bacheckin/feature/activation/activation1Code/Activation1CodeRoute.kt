package de.jscoding.bacheckin.feature.activation.activation1Code

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.jscoding.bacheckin.core.ui.components.QRCodeScanner
import de.jscoding.bacheckin.feature.activation.ActivationEvent
import de.jscoding.bacheckin.feature.activation.ActivationViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalGetImage::class)
@Composable
fun Activation1CodeRoute(
    viewModel: ActivationViewModel,
    onNext: () -> Unit,
    onCancel: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var isQrScannerOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                ActivationEvent.CodeAccepted -> onNext()
                ActivationEvent.ActivationCancelled -> onCancel()
                is ActivationEvent.ShowSnackbar -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(event.message)
                }

                else -> Unit
            }
        }
    }

    Activation1CodeScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onActivationCodeChange = viewModel::onActivationCodeChange,
        onOpenQRScanner = { isQrScannerOpen = true },
        onSubmit = viewModel::validateActivationCode,
    )

    if (isQrScannerOpen) {
        QRCodeScanner(
            onBarcodeScanned = { qrCode ->
                val activationCode = getActivationCodeFromQRCode(qrCode)
                if (activationCode != null) {
                    viewModel.onActivationCodeChange(activationCode)
                } else {
                    scope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar("Ungültiger QR-Code: $qrCode")
                    }
                }
                isQrScannerOpen = false
            },
            onDismiss = { isQrScannerOpen = false }
        )
    }
}

private fun getActivationCodeFromQRCode(scannedQrCode: String): String? =
    if (scannedQrCode.startsWith("ba-checkin://")) {
        runCatching { scannedQrCode.toUri().getQueryParameter("code") }.getOrNull()
    } else {
        null
    }
