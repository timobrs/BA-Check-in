package de.jscoding.bacheckin.feature.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.jscoding.bacheckin.R
import de.jscoding.bacheckin.core.settings.AppSettings
import de.jscoding.bacheckin.core.settings.ThemePreference
import de.jscoding.bacheckin.core.ui.components.BCICard
import de.jscoding.bacheckin.core.ui.components.BCIList
import de.jscoding.bacheckin.core.ui.components.BCIListEntry
import de.jscoding.bacheckin.core.ui.components.BCISection
import de.jscoding.bacheckin.core.ui.components.BCISegmentedSelector
import de.jscoding.bacheckin.core.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    appVersion: String,
    onBack: () -> Unit,
    onThemePreferenceSelected: (ThemePreference) -> Unit,
    onBiometricPreferredChanged: (Boolean) -> Unit,
    onRemoveDeviceBinding: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRemoveDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Einstellungen",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            BCISection(
                title = "Darstellung"
            ) {
                BCICard {
                    BCISegmentedSelector(
                        modifier = Modifier.padding(vertical = 4.dp),
                        options = ThemePreference.entries,
                        selectedOption = uiState.appSettings.themePreference,
                        onOptionSelected = onThemePreferenceSelected,
                        labelProvider = { theme ->
                            when (theme) {
                                ThemePreference.SYSTEM -> "System"
                                ThemePreference.LIGHT -> "Hell"
                                ThemePreference.DARK -> "Dunkel"
                            }
                        }
                    )
                }
            }

            val accountListEntries: MutableList<@Composable (RowScope.() -> Unit)> = mutableListOf()
            if (uiState.canUseBiometricAuth) {
                accountListEntries += {
                    BCIListEntry.SwitchRow(
                        verticalPadding = 8.dp,
                        title = "Anmelden mit Biometrischen Daten",
                        checked = uiState.appSettings.biometricPreferred,
                        onCheckedChange = onBiometricPreferredChanged
                    )
                }
            }
            accountListEntries += {
                BCIListEntry.DefaultRow(
                    title = "Gerätebindung entfernen",
                    onClick = { showRemoveDialog = true },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }

            BCISection(
                title = "Account"
            ) {
                BCIList(
                    innerPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    listEntries = accountListEntries
                )
            }

            BCISection(
                title = "Rechtliches",
            ) {
                BCIList(
                    innerPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    listEntries = listOf(
                        {
                            BCIListEntry.ClickableRow(
                                title = "Impressum",
                                onClick = {}
                            )
                        },
                        {
                            BCIListEntry.ClickableRow(
                                title = "Datenschutzerklärung",
                                onClick = {}
                            )
                        },
                        {
                            BCIListEntry.ClickableRow(
                                title = "Nutzungsbedingungen",
                                onClick = {}
                            )
                        },
                        {
                            BCIListEntry.DefaultRow(
                                title = "Version",
                                trailingContent = {
                                    Text(
                                        text = appVersion,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            )
                        }
                    )
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Image(
                painter = painterResource(R.drawable.ba_big_logo),
                contentDescription = "BA Logo",
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Gerätebindung entfernen?") },
            text = {
                Text(
                    "Dadurch werden lokale Tokens und Geräteschlüssel gelöscht. " +
                            "Für die erneute Nutzung brauchst du einen neuen Freischaltungscode " +
                            "aus dem Sekretariat."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveDialog = false
                        onRemoveDeviceBinding()
                    }
                ) {
                    Text("Entfernen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    AppTheme {
        SettingsScreen(
            uiState = SettingsUiState(
                appSettings = AppSettings(
                    themePreference = ThemePreference.SYSTEM,
                    biometricPreferred = true,
                )
            ),
            appVersion = "Vorschau",
            onBack = {},
            onThemePreferenceSelected = {},
            onBiometricPreferredChanged = {},
            onRemoveDeviceBinding = {}
        )
    }
}
