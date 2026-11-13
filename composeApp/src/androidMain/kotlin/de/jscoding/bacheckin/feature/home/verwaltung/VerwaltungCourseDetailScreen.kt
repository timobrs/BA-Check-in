package de.jscoding.bacheckin.feature.home.verwaltung

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.jscoding.bacheckin.core.ui.components.BCIList
import de.jscoding.bacheckin.core.ui.components.BCIListEntry
import de.jscoding.bacheckin.core.ui.components.BCISection
import de.jscoding.bacheckin.core.ui.theme.AppTheme
import de.jscoding.bacheckin.feature.home.Appointment
import de.jscoding.bacheckin.feature.home.AttendanceStatus
import de.jscoding.bacheckin.feature.home.StudentAttendance
import de.jscoding.bacheckin.feature.home.VerwaltungAttendanceDay
import de.jscoding.bacheckin.feature.home.VerwaltungCourse
import de.jscoding.bacheckin.feature.home.VerwaltungCourseDetail
import de.jscoding.bacheckin.feature.home.VerwaltungModule
import de.jscoding.bacheckin.feature.home.VerwaltungUser
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerwaltungCourseDetailScreen(
    modifier: Modifier = Modifier,
    courseDetail: VerwaltungCourseDetail,
    isLoading: Boolean,
    onBack: () -> Unit,
    onOpenUser: (String, String) -> Unit,
    onOpenAttendances: (String, UUID) -> Unit,
    onRefreshCalendar: () -> Unit,
    onSetAppointmentIgnored: (UUID, Boolean) -> Unit,
    onSetModuleIgnored: (String, Boolean) -> Unit,
    onSetModuleLecturer: (String, String?) -> Unit = { _, _ -> },
    onSetAppointmentLecturer: (UUID, Boolean, String?) -> Unit = { _, _, _ -> }
) {
    val tabs = VerwaltungCourseTab.entries
    val pagerState = rememberPagerState { tabs.size }
    val coroutineScope = rememberCoroutineScope()
    var selectedAppointment by remember { mutableStateOf<Appointment?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            VerwaltungDetailTopBar(
                title = courseDetail.course.name,
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
                    when (tabs[page]) {
                        VerwaltungCourseTab.Verwaltung -> item {
                            BCISection(title = "Alle Nutzer") {
                                BCIList(
                                    listEntries = courseDetail.users.map { user ->
                                        {
                                            BCIListEntry.ClickableRow(
                                                title = user.displayName,
                                                subtitle = user.studentId,
                                                onClick = {
                                                    onOpenUser(courseDetail.course.id, user.studentId)
                                                }
                                            )
                                        }
                                    }
                                )
                            }
                        }

                        VerwaltungCourseTab.Anwesenheiten -> item {
                            BCISection(title = "Tage") {
                                BCIList(
                                    listEntries = courseDetail.attendanceDays.map { day ->
                                        {
                                            VerwaltungAttendanceDayRow(
                                                day = day,
                                                onClick = {
                                                    onOpenAttendances(courseDetail.course.id, day.appointment.id)
                                                }
                                            )
                                        }
                                    },
                                    innerPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                                )
                            }
                        }

                        VerwaltungCourseTab.Termine -> {
                            val allModules = courseDetail.availableModules.filter {
                                it.courseGroupId == courseDetail.course.id
                            }
                            val now = Instant.now()
                            val zone = ZoneId.systemDefault()
                            val today = now.atZone(zone).toLocalDate()
                            val windowStart = today.atStartOfDay(zone).toInstant()
                            val windowEnd = today.plusDays(7).atStartOfDay(zone).toInstant()
                            val ignoredModuleIds = allModules.filter { it.ignored }.map { it.id }.toSet()
                            val regularAppointments = courseDetail.appointments.filter {
                                !it.ignored && it.moduleId !in ignoredModuleIds &&
                                        !it.startAt.isBefore(windowStart) &&
                                        it.endAt.isAfter(now) && it.startAt.isBefore(windowEnd)
                            }.sortedBy { it.startAt }
                            val ignoredAppointments = courseDetail.appointments.filter {
                                it.ignored && it.moduleId !in ignoredModuleIds && it.endAt.isAfter(now)
                            }
                            val ignoredModules = allModules.filter { module ->
                                module.ignored && courseDetail.appointments.any {
                                    it.moduleId == module.id && it.endAt.isAfter(now)
                                }
                            }
                            item {
                                Button(
                                    onClick = onRefreshCalendar,
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isLoading
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(Icons.Outlined.Autorenew, contentDescription = null)
                                    }
                                    Text(
                                        text = "Terminkalender aktualisieren",
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                            if (regularAppointments.isNotEmpty()) item {
                                BCISection(title = "Termine in den nächsten 7 Tagen") {
                                    BCIList(listEntries = regularAppointments.map { appointment ->
                                        {
                                            BCIListEntry.DefaultRow(
                                                title = appointment.title,
                                                subtitle = allModules.firstOrNull { it.id == appointment.moduleId }?.name
                                                    ?: "Kein Modul",
                                                onClick = { selectedAppointment = appointment },
                                                trailingContent = {
                                                    if (appointment.lecturerId == null) {
                                                        Icon(Icons.Outlined.PersonOff, "Dozentenzuweisung fehlt",
                                                            tint = MaterialTheme.colorScheme.error)
                                                    }
                                                }
                                            )
                                        }
                                    })
                                }
                            }
                            if (ignoredAppointments.isNotEmpty()) item {
                                BCISection(title = "Ignorierte Termine") {
                                    BCIList(listEntries = ignoredAppointments.map { appointment ->
                                        {
                                            BCIListEntry.DefaultRow(
                                                title = appointment.title,
                                                subtitle = allModules.firstOrNull { it.id == appointment.moduleId }?.name
                                                    ?: "Kein Modul",
                                                onClick = { selectedAppointment = appointment },
                                                trailingContent = {
                                                    if (appointment.lecturerId == null) {
                                                        Icon(Icons.Outlined.PersonOff, "Dozentenzuweisung fehlt",
                                                            tint = MaterialTheme.colorScheme.error)
                                                    }
                                                }
                                            )
                                        }
                                    })
                                }
                            }
                            if (ignoredModules.isNotEmpty()) item {
                                BCISection(title = "Ignorierte Kurse") {
                                    BCIList(listEntries = ignoredModules.mapNotNull { module ->
                                        val appointment = courseDetail.appointments.firstOrNull {
                                            it.moduleId == module.id && it.endAt.isAfter(now)
                                        } ?: return@mapNotNull null
                                        {
                                            BCIListEntry.DefaultRow(
                                                title = module.name,
                                                onClick = { selectedAppointment = appointment },
                                                trailingContent = {
                                                    if (module.lecturerId == null || courseDetail.lecturers.none {
                                                            it.id == module.lecturerId && it.active
                                                        }) {
                                                        Icon(Icons.Outlined.PersonOff, "Dozentenzuweisung fehlt",
                                                            tint = MaterialTheme.colorScheme.error)
                                                    }
                                                }
                                            )
                                        }
                                    })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedAppointment?.let { selected ->
        val appointment = courseDetail.appointments.firstOrNull { it.id == selected.id } ?: selected
        val selectedModule = courseDetail.availableModules.firstOrNull { it.id == appointment.moduleId }
        VerwaltungAppointmentSheet(
            appointment = appointment,
            moduleName = selectedModule?.name,
            moduleIgnored = selectedModule?.ignored == true,
            lecturers = courseDetail.lecturers,
            moduleLecturerId = selectedModule?.lecturerId,
            isLoading = isLoading,
            onClose = { selectedAppointment = null },
            onIgnoredChange = { ignored ->
                selectedAppointment = appointment.copy(ignored = ignored)
                onSetAppointmentIgnored(appointment.id, ignored)
            },
            onModuleIgnoredChange = { ignored ->
                selectedModule?.let { onSetModuleIgnored(it.id, ignored) }
            },
            onModuleLecturerChange = { lecturerId ->
                selectedModule?.let { onSetModuleLecturer(it.id, lecturerId) }
            },
            onAppointmentLecturerChange = { overrideModule, lecturerId ->
                onSetAppointmentLecturer(appointment.id, overrideModule, lecturerId)
            }
        )
    }
}

@Preview
@Composable
private fun VerwaltungCourseDetailScreenPreview() {
    AppTheme {
        VerwaltungCourseDetailScreen(
            courseDetail = previewCourseDetail(),
            isLoading = false,
            onBack = {},
            onOpenUser = { _, _ -> },
            onOpenAttendances = { _, _ -> },
            onRefreshCalendar = {},
            onSetAppointmentIgnored = { _, _ -> },
            onSetModuleIgnored = { _, _ -> }
        )
    }
}

private fun previewCourseDetail(): VerwaltungCourseDetail {
    val now = Instant.now()
    val appointments = listOf(
        Appointment(
            title = "Einführung in die Informatik",
            room = "203",
            startAt = now.minusSeconds(30 * 60L),
            endAt = now.plusSeconds(90 * 60L),
            course = "AI-WS24-IV",
            protokollant = "Max Mustermann",
            moduleId = "m1"
        ),
        Appointment(
            title = "Datenbanken & Architekturen",
            room = "102",
            startAt = now.plusSeconds(3 * 60 * 60L),
            endAt = now.plusSeconds(6 * 60 * 60L),
            course = "AI-WS24-IV",
            protokollant = "Maja Müller",
            moduleId = "m2"
        )
    )
    val users = listOf(
        VerwaltungUser("123456", "Max Mustermann", Instant.now(), null),
        VerwaltungUser("123457", "Aylin Demir", Instant.now(), "BA-123457")
    )
    return VerwaltungCourseDetail(
        course = VerwaltungCourse("AI-WS24-IV", "AI-WS24-IV"),
        users = users,
        appointments = appointments,
        attendanceDays = appointments.map { appointment ->
            VerwaltungAttendanceDay(
                appointment = appointment,
                attendances = users.map { user ->
                    StudentAttendance(user.studentId, user.displayName, AttendanceStatus.Present)
                },
                attendancesValidated = true
            )
        },
        availableModules = listOf(
            VerwaltungModule(id = "m1", courseGroupId = "AI-WS24-IV", name = "Software Engineering"),
            VerwaltungModule(id = "m2", courseGroupId = "AI-WS24-IV", name = "Datenbanken")
        )
    )
}
