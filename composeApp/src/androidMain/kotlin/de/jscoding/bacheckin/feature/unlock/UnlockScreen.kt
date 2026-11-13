package de.jscoding.bacheckin.feature.unlock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.jscoding.bacheckin.core.ui.components.BCIPinDots
import de.jscoding.bacheckin.core.ui.components.BCIPinInputBox

@Composable
fun UnlockScreen(
    uiState: UnlockUiState,
    modifier: Modifier = Modifier,
    onUnlock: () -> Unit,
    onPinChange: (String) -> Unit,
    canUseBiometricAuth: Boolean = false
) {
    Scaffold(modifier = modifier) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "App entsperren",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (canUseBiometricAuth) {
                    "Gib deine im Setup gesetzte PIN ein oder nutze die Biometrie unten links."
                } else {
                    "Gib deine im Setup gesetzte PIN ein."
                }
            )
            uiState.errorMessage?.let {
                Spacer(Modifier.height(16.dp))
                Text(text = it, color = colorScheme.error)
            }
            Spacer(Modifier.height(24.dp))
            BCIPinDots(
                filledDots = uiState.pin.length,
                isError = uiState.pinError
            )
            Spacer(Modifier.height(24.dp))
            BCIPinInputBox(
                modifier = Modifier.fillMaxWidth(),
                pin = uiState.pin,
                onPinChange = onPinChange,
                isLoading = uiState.isLoading,
                bottomLeftIcon = if (canUseBiometricAuth) Icons.Filled.Fingerprint else null,
                bottomLeftIconTint = colorScheme.primary,
                bottomLeftIconAction = if (canUseBiometricAuth) onUnlock else null
            )
            Box(Modifier.height(48.dp)) {
                if (uiState.isLoading) {
                    Spacer(Modifier.height(24.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}
