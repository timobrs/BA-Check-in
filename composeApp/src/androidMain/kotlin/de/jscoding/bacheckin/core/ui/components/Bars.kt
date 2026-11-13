package de.jscoding.bacheckin.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.jscoding.bacheckin.core.ui.theme.AppTheme
import de.jscoding.bacheckin.core.ui.theme.LocalCustomColors
import java.time.LocalTime


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BCIWelcomeBar(
    title: String = getGreeting(),
    subtitle: String? = null,
    onOpenSettings: () -> Unit,
    actionsBesideSettings: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        actions = {
            actionsBesideSettings()

            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
    )
}

private fun getGreeting(): String {
    val hour = LocalTime.now().hour
    return when (hour) {
        in 2..10 -> "Guten Morgen"
        in 11..18 -> "Guten Tag"
        else -> "Guten Abend"
    }
}

@Composable
fun SessionStatus(
    modifier: Modifier = Modifier,
    inactivityRemainingMs: Long,
    onManualRefresh: () -> Unit,
) {
    val locale = LocalLocale.current.platformLocale
    val customColors = LocalCustomColors.current
    val seconds = ((inactivityRemainingMs.coerceAtLeast(0L) + 999L) / 1000L)
    val minutesPart = seconds / 60L
    val secondsPart = seconds % 60L
    val label = String.format(locale, "%d:%02d", minutesPart, secondsPart)
    val contentColor = if (inactivityRemainingMs > 60_000L) {
        customColors.success
    } else {
        customColors.warning
    }

    TextButton(
        modifier = modifier,
        onClick = onManualRefresh,
        colors = ButtonDefaults.textButtonColors(
            contentColor = contentColor
        )
    ) {
        Icon(
            imageVector = Icons.Outlined.Autorenew,
            contentDescription = "Session aktualisieren",
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(text = label, maxLines = 1)
    }
}

@Preview
@Composable
private fun BCIWelcomeBarPreview() {
    AppTheme {
        BCIWelcomeBar(
            title = "Guten Morgen",
            subtitle = "Matrikelnummer: 123456",
            onOpenSettings = {},
            actionsBesideSettings = {
                SessionStatus(
                    inactivityRemainingMs = 300_000,
                    onManualRefresh = {},
                )
            }
        )
    }
}

@Preview
@Composable
private fun BCIWelcomeBarInactivityWarningPreview() {
    AppTheme {
        BCIWelcomeBar(
            title = "Guten Morgen",
            subtitle = "Matrikelnummer: 123456",
            onOpenSettings = {},
            actionsBesideSettings = {
                SessionStatus(
                    inactivityRemainingMs = 59_000,
                    onManualRefresh = {},
                )
            }
        )
    }
}
