package de.jscoding.bacheckin.feature.home.verwaltung

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.jscoding.bacheckin.core.ui.components.BCIList
import de.jscoding.bacheckin.core.ui.components.BCISection
import de.jscoding.bacheckin.core.ui.theme.AppTheme
import de.jscoding.bacheckin.feature.home.Appointment
import de.jscoding.bacheckin.feature.home.AttendanceStatus
import de.jscoding.bacheckin.feature.home.VerwaltungCourse
import de.jscoding.bacheckin.feature.home.VerwaltungCourseDetail
import de.jscoding.bacheckin.feature.home.VerwaltungModule
import de.jscoding.bacheckin.feature.home.VerwaltungUser
import de.jscoding.bacheckin.feature.home.VerwaltungUserAttendanceDay
import de.jscoding.bacheckin.feature.home.VerwaltungUserDetail
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerwaltungUserDetailScreen(
    modifier: Modifier = Modifier,
    course: VerwaltungCourseDetail,
    userDetail: VerwaltungUserDetail,
    onBack: () -> Unit,
    onOpenAttendances: (String, UUID) -> Unit,
    onCreateActivationCode: (String) -> Unit,
    onRevokeDevice: (String) -> Unit,
    onSetStudentModule: (String, String, Boolean) -> Unit
) {
    val tabs = VerwaltungUserTab.entries
    val pagerState = rememberPagerState { tabs.size }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        topBar = {
            VerwaltungDetailTopBar(
                title = userDetail.user.displayName,
                onBack = onBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            PrimaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier.padding(16.dp)
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(tab.label) }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        when (tabs[page]) {
                            VerwaltungUserTab.Nutzerverwaltung -> VerwaltungUserManagementCard(
                                user = userDetail.user,
                                onCreateActivationCode = onCreateActivationCode,
                                onRevokeDevice = onRevokeDevice
                            )

                            VerwaltungUserTab.Module -> BCISection(title = "Individuelle Module") {
                                BCIList(
                                    listEntries = userDetail.availableModules
                                        .filter { it.courseGroupId != course.course.id }
                                        .map { module ->
                                        {
                                            Row {
                                                Checkbox(
                                                    checked = module.id in userDetail.assignedModuleIds,
                                                    onCheckedChange = { assigned ->
                                                        onSetStudentModule(userDetail.user.studentId, module.id, assigned)
                                                    }
                                                )
                                                Text(module.name, modifier = Modifier.padding(top = 12.dp))
                                            }
                                        }
                                    },
                                    innerPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                                )
                            }

                            VerwaltungUserTab.Anwesenheiten -> BCISection(title = "Tage") {
                                BCIList(
                                    listEntries = userDetail.attendanceDays.map { day ->
                                        {
                                            VerwaltungUserAttendanceDayRow(
                                                day = day,
                                                onClick = {
                                                    onOpenAttendances(
                                                        course.course.id,
                                                        day.appointment.id
                                                    )
                                                }
                                            )
                                        }
                                    },
                                    innerPadding = PaddingValues(
                                        horizontal = 16.dp,
                                        vertical = 0.dp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun VerwaltungUserDetailScreenPreview() {
    AppTheme {
        val courseDetail = previewCourseDetail()
        val userDetail = previewUserDetail()

        VerwaltungUserDetailScreen(
            course = courseDetail,
            userDetail = userDetail,
            onBack = {},
            onOpenAttendances = { _, _ -> },
            onCreateActivationCode = {},
            onRevokeDevice = {},
            onSetStudentModule = { _, _, _ -> }
        )
    }
}

private fun previewCourseDetail(): VerwaltungCourseDetail {
    return VerwaltungCourseDetail(
        course = VerwaltungCourse("AI-WS24-IV", "AI-WS24-IV"),
        users = emptyList(),
        appointments = emptyList(),
        attendanceDays = emptyList(),
        availableModules = listOf(
            VerwaltungModule(id = "m1", courseGroupId = "AI-WS24-IV", name = "Software Engineering"),
            VerwaltungModule(id = "m2", courseGroupId = "OTHER", name = "Wahlpflichtfach KI")
        )
    )
}

private fun previewUserDetail(): VerwaltungUserDetail {
    val now = Instant.now()
    val appointment = Appointment(
        title = "Einführung in die Informatik",
        room = "203",
        startAt = now,
        endAt = now.plusSeconds(3600),
        course = "AI-WS24-IV",
        protokollant = "Max Mustermann"
    )

    return VerwaltungUserDetail(
        user = VerwaltungUser(
            studentId = "123456",
            displayName = "Max Mustermann",
            activatedSince = Instant.now(),
            generatedActivationCode = "BA-123456"
        ),
        attendanceDays = listOf(
            VerwaltungUserAttendanceDay(
                appointment = appointment,
                status = AttendanceStatus.Present,
                lateMinutes = null
            ),
            VerwaltungUserAttendanceDay(
                appointment = appointment.copy(title = "Datenbanken"),
                status = AttendanceStatus.Late,
                lateMinutes = 10
            )
        ),
        availableModules = listOf(
            VerwaltungModule(id = "m2", courseGroupId = "OTHER", name = "Wahlpflichtfach KI")
        ),
        assignedModuleIds = setOf("m2")
    )
}
