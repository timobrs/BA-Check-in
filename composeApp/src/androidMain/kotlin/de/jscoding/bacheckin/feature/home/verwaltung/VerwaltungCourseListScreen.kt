package de.jscoding.bacheckin.feature.home.verwaltung

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.jscoding.bacheckin.core.ui.components.BCIList
import de.jscoding.bacheckin.core.ui.components.BCIListEntry
import de.jscoding.bacheckin.core.ui.components.BCISection
import de.jscoding.bacheckin.core.ui.components.BCIWelcomeBar
import de.jscoding.bacheckin.core.ui.theme.AppTheme
import de.jscoding.bacheckin.feature.home.HomeUiState
import de.jscoding.bacheckin.feature.home.VerwaltungAbsenceReview
import de.jscoding.bacheckin.feature.home.VerwaltungCourse
import java.time.Instant

@Composable
fun VerwaltungCourseListScreen(
    modifier: Modifier = Modifier,
    uiState: HomeUiState,
    onOpenSettings: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenCourse: (String) -> Unit,
    onRefreshCalendar: () -> Unit,
    onReviewAbsence: (String, Boolean) -> Unit,
    actionsBesideSettings: @Composable RowScope.() -> Unit = {}
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            BCIWelcomeBar(
                subtitle = uiState.userDisplayName?.takeIf { it.isNotBlank() },
                onOpenSettings = onOpenSettings,
                actionsBesideSettings = actionsBesideSettings
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onRefreshCalendar,
                icon = {
                    if (uiState.refreshIsLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Outlined.Autorenew, contentDescription = null)
                    }
                },
                text = { Text("Kalender aktualisieren") }
            )
        }
    ) { paddingValues ->
        val filteredCourses = uiState.verwaltungCourses.filterBy(
            query = uiState.verwaltungSearchQuery,
            courseDetails = uiState.verwaltungCourseDetails
        )

        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = uiState.verwaltungSearchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = null)
                    },
                    placeholder = { Text("Nutzer oder Kurs eingeben") }
                )
            }

            if (uiState.verwaltungAbsenceReviews.isNotEmpty()) item {
                BCISection(title = "Offene Abwesenheiten") {
                    BCIList(listEntries = uiState.verwaltungAbsenceReviews.map { review ->
                        {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(review.studentName, modifier = Modifier.weight(1f))
                                TextButton(onClick = { onReviewAbsence(review.id, false) }) { Text("Ablehnen") }
                                TextButton(onClick = { onReviewAbsence(review.id, true) }) { Text("Freigeben") }
                            }
                        }
                    })
                }
            }

            item {
                BCISection(title = "Alle Kurse") {
                    BCIList(
                        listEntries = filteredCourses.map { course ->
                            {
                                BCIListEntry.ClickableRow(
                                    title = course.name,
                                    onClick = { onOpenCourse(course.id) }
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun VerwaltungCourseListScreenPreview() {
    AppTheme {
        VerwaltungCourseListScreen(
            uiState = HomeUiState(
                verwaltungCourses = listOf(
                    VerwaltungCourse(id = "AI-WS24-IV", name = "AI-WS24-IV"),
                    VerwaltungCourse(id = "AI-WS24-II", name = "AI-WS24-II"),
                    VerwaltungCourse(id = "AI-WS22-III", name = "AI-WS22-III")
                ),
                verwaltungAbsenceReviews = listOf(
                    VerwaltungAbsenceReview(
                        id = "1",
                        studentName = "Max Mustermann",
                        fromAt = Instant.now(),
                        untilAt = Instant.now()
                    ),
                    VerwaltungAbsenceReview(
                        id = "2",
                        studentName = "Aylin Demir",
                        fromAt = Instant.now(),
                        untilAt = Instant.now()
                    )
                )
            ),
            onOpenSettings = {},
            onSearchQueryChange = {},
            onOpenCourse = {},
            onRefreshCalendar = {},
            onReviewAbsence = { _, _ -> },
            actionsBesideSettings = {}
        )
    }
}
