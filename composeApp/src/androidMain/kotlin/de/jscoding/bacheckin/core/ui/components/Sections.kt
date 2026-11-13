package de.jscoding.bacheckin.core.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun BCISection(
    modifier: Modifier = Modifier,
    title: String?,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
    ) {
        title?.let {
            Text(
                text = it,
                style = typography.titleMedium,
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
        }
        content()
    }
}

@Composable
fun BCIList(
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
    listEntries: List<@Composable RowScope.() -> Unit>
) {
    BCICard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .animateContentSize()
                .padding(innerPadding)
        ) {
            listEntries.forEachIndexed { index, listEntry ->
                Row { listEntry() }
                if (index != listEntries.size - 1) {
                    BCIDivider()
                }
            }
        }
    }
}

object BCIListEntry {
    @Composable
    fun DefaultRow(
        modifier: Modifier = Modifier,
        verticalPadding: Dp = 16.dp,
        title: String,
        subtitle: String? = null,
        titleMaxLines: Int = 1,
        subtitleMaxLines: Int = 1,
        onClick: (() -> Unit)? = null,
        trailingContent: @Composable (RowScope.() -> Unit)? = null,
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clickable(enabled = onClick != null, onClick = { onClick?.let { it() } })
                .padding(vertical = verticalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = title,
                    style = typography.bodyLarge,
                    maxLines = titleMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = subtitleMaxLines,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            trailingContent?.let {
                it()
            }
        }
    }

    @Composable
    fun SwitchRow(
        modifier: Modifier = Modifier,
        title: String,
        subtitle: String? = null,
        titleMaxLines: Int = 1,
        subtitleMaxLines: Int = 1,
        verticalPadding: Dp = 16.dp,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
    ) {
        DefaultRow(
            modifier = modifier,
            title = title,
            subtitle = subtitle,
            titleMaxLines = titleMaxLines,
            subtitleMaxLines = subtitleMaxLines,
            verticalPadding = verticalPadding,
            trailingContent = {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange
                )
            }
        )
    }

    @Composable
    fun ClickableRow(
        modifier: Modifier = Modifier,
        title: String,
        subtitle: String? = null,
        titleMaxLines: Int = 1,
        subtitleMaxLines: Int = 1,
        showArrow: Boolean = true,
        onClick: () -> Unit,
    ) {
        DefaultRow(
            modifier = modifier,
            title = title,
            subtitle = subtitle,
            titleMaxLines = titleMaxLines,
            subtitleMaxLines = subtitleMaxLines,
            onClick = onClick,
            trailingContent = {
                if (showArrow) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
}

