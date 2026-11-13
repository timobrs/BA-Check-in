package de.jscoding.bacheckin.feature.activation.activation1Code

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.jscoding.bacheckin.core.ui.components.BCIAuthButton
import de.jscoding.bacheckin.feature.activation.ActivationUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Activation1CodeScreen(
    uiState: ActivationUiState,
    snackbarHostState: SnackbarHostState,
    onActivationCodeChange: (String) -> Unit,
    onOpenQRScanner: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Gerät freischalten") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Freischaltungscode",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Gib den Code ein, den du im Sekretariat erhalten hast. Es wird kein Benutzername und kein Passwort verwendet.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = uiState.activationCode,
                onValueChange = onActivationCodeChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Freischaltungscode") },
                singleLine = true,
                maxLines = 2,
                enabled = !uiState.isLoading,
                isError = uiState.activationCodeError != null,
                supportingText = {
                    uiState.activationCodeError?.let { Text(it) }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters
                ),
                trailingIcon = {
                    IconButton(
                        onClick = onOpenQRScanner
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "QR Code scannen"
                        )
                    }
                }
            )

            Spacer(Modifier.height(24.dp))
            BCIAuthButton(
                text = "Weiter",
                enabled = !uiState.isLoading,
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth()
            )

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Preview
@Composable
private fun Activation1CodeScreenPreview() {
    Activation1CodeScreen(
        uiState = ActivationUiState(
            activationCode = "ABCDE",
            activationCodeError = "Der Freischaltungscode ist zu kurz.",
            isLoading = false
        ),
        snackbarHostState = SnackbarHostState(),
        onActivationCodeChange = {},
        onOpenQRScanner = {},
        onSubmit = {},
    )
}
