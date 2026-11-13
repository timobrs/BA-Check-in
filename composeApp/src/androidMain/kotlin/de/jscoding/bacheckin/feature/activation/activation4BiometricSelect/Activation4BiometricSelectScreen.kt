package de.jscoding.bacheckin.feature.activation.activation4BiometricSelect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.jscoding.bacheckin.core.ui.components.BCIAuthButton
import de.jscoding.bacheckin.feature.activation.ActivationUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Activation4BiometricSelectScreen(
    uiState: ActivationUiState,
    snackbarHostState: SnackbarHostState,
    onBiometricPreferredChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Entsperrung") },
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
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = if (uiState.canUseBiometricAuth) "Biometrie verwenden (optional)" else "Entsperrung einrichten",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (uiState.canUseBiometricAuth) {
                    "Du kannst die Biometrie (Fingerabdruck / Gesichtserkennung) für schnelles Entsperren aktivieren oder die Freischaltung direkt nur mit deiner 5-stelligen PIN abschließen."
                } else {
                    "Dein Gerät hat keine sichere Biometrie/Gerätesperre eingerichtet. Du kannst die App trotzdem vollumfänglich mit deiner 5-stelligen PIN nutzen."
                },
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Biometrie aktivieren")
                Switch(
                    checked = uiState.biometricPreferred,
                    onCheckedChange = onBiometricPreferredChange,
                    enabled = !uiState.isLoading && uiState.canUseBiometricAuth
                )
            }
            Spacer(Modifier.height(24.dp))
            BCIAuthButton(
                text = "Gerät freischalten",
                onClick = onSubmit,
                enabled = !uiState.isLoading,
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
private fun Activation4BiometricSelectScreenPreview() {
    Activation4BiometricSelectScreen(
        uiState = ActivationUiState(),
        snackbarHostState = SnackbarHostState(),
        onBiometricPreferredChange = {},
        onSubmit = {},
        onBack = {},
        onCancel = {}
    )
}
