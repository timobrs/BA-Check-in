package de.jscoding.bacheckin.feature.activation.activation3PinValidate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.jscoding.bacheckin.core.ui.components.BCIPinDots
import de.jscoding.bacheckin.core.ui.components.BCIPinInputBox
import de.jscoding.bacheckin.feature.activation.ActivationUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Activation3PinValidateScreen(
    uiState: ActivationUiState,
    snackbarHostState: SnackbarHostState,
    onConfirmationPinChange: (String) -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("PIN bestätigen") },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !uiState.isLoading) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = onCancel,
                        enabled = !uiState.isLoading
                    ) {
                        Text("Abbrechen")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Bestätige deine lokale PIN", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(24.dp))
            BCIPinDots(
                filledDots = uiState.confirmationPin.length,
                isError = uiState.confirmationPinError
            )
            if (uiState.confirmationPinError) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Die PINs stimmen nicht überein.",
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(48.dp))
            BCIPinInputBox(
                pin = uiState.confirmationPin,
                onPinChange = onConfirmationPinChange,
                isLoading = uiState.isLoading,
                bottomLeftIcon = Icons.AutoMirrored.Filled.ArrowBack,
                bottomLeftIconTint = MaterialTheme.colorScheme.error,
                bottomLeftIconAction = onBack,
                maxLength = 5
            )
        }
    }
}

@Preview
@Composable
private fun Activation3PinValidateScreenPreview() {
    Activation3PinValidateScreen(
        uiState = ActivationUiState(),
        snackbarHostState = SnackbarHostState(),
        onConfirmationPinChange = {},
        onBack = {},
        onCancel = {}
    )
}
