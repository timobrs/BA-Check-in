package de.jscoding.bacheckin.feature.home.verwaltung

import android.net.Uri
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import de.jscoding.bacheckin.core.ui.theme.AppTheme
import de.jscoding.bacheckin.feature.home.Appointment
import de.jscoding.bacheckin.feature.home.AttendanceStatus
import de.jscoding.bacheckin.feature.home.HomeSheet
import de.jscoding.bacheckin.feature.home.HomeUiState
import de.jscoding.bacheckin.feature.home.StudentAttendance
import de.jscoding.bacheckin.feature.home.VerwaltungAttendanceDay
import de.jscoding.bacheckin.feature.home.VerwaltungCourse
import de.jscoding.bacheckin.feature.home.VerwaltungCourseDetail
import de.jscoding.bacheckin.feature.home.VerwaltungModule
import de.jscoding.bacheckin.feature.home.VerwaltungUser
import de.jscoding.bacheckin.feature.home.VerwaltungUserAttendanceDay
import de.jscoding.bacheckin.feature.home.VerwaltungUserDetail
import java.time.Instant
import java.util.UUID

@Composable
fun VerwaltungHomeScreen(
    modifier: Modifier = Modifier,
    uiState: HomeUiState,
    onOpenSettings: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenAttendances: (String, UUID) -> Unit,
    onOpenAttendanceEdit: () -> Unit,
    onUpdateAttendance: (UUID, String, AttendanceStatus) -> Unit,
    onValidateAttendances: (UUID) -> Unit,
    onCreateActivationCode: (String) -> Unit,
    onRevokeDevice: (String) -> Unit,
    onSetStudentModule: (String, String, Boolean) -> Unit,
    onSetAppointmentIgnored: (UUID, Boolean) -> Unit,
    onSetModuleIgnored: (String, Boolean) -> Unit,
    onSetModuleLecturer: (String, String?) -> Unit = { _, _ -> },
    onSetAppointmentLecturer: (UUID, Boolean, String?) -> Unit = { _, _, _ -> },
    onReviewAbsence: (String, Boolean) -> Unit,
    onLoadBackendData: () -> Unit,
    onRefreshCalendar: () -> Unit,
    onCloseSheet: () -> Unit,
    actionsBesideSettings: @Composable RowScope.() -> Unit = {}
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = VerwaltungDestination.CourseList.route,
        modifier = modifier
    ) {
        composable(VerwaltungDestination.CourseList.route) {
            VerwaltungCourseListScreen(
                modifier = Modifier,
                uiState = uiState,
                onOpenSettings = onOpenSettings,
                onSearchQueryChange = onSearchQueryChange,
                onOpenCourse = { courseId ->
                    onLoadBackendData()
                    navController.navigate(VerwaltungDestination.CourseDetail.createRoute(courseId))
                },
                onRefreshCalendar = onRefreshCalendar,
                onReviewAbsence = onReviewAbsence,
                actionsBesideSettings = actionsBesideSettings
            )
        }

        composable(
            route = VerwaltungDestination.CourseDetail.route,
            arguments = listOf(navArgument(VerwaltungDestination.COURSE_ID_ARGUMENT) { type = NavType.StringType })
        ) { entry ->
            val courseId = entry.arguments?.getString(VerwaltungDestination.COURSE_ID_ARGUMENT).orEmpty()
            val courseDetail = uiState.verwaltungCourseDetails[courseId]

            if (courseDetail != null) {
                VerwaltungCourseDetailScreen(
                    modifier = Modifier,
                    courseDetail = courseDetail,
                    isLoading = uiState.refreshIsLoading,
                    onBack = { navController.popBackStack() },
                    onOpenUser = { selectedCourseId, studentId ->
                        navController.navigate(
                            VerwaltungDestination.UserDetail.createRoute(
                                courseId = selectedCourseId,
                                studentId = studentId
                            )
                        )
                    },
                    onOpenAttendances = onOpenAttendances,
                    onRefreshCalendar = onRefreshCalendar,
                    onSetAppointmentIgnored = onSetAppointmentIgnored,
                    onSetModuleIgnored = onSetModuleIgnored,
                    onSetModuleLecturer = onSetModuleLecturer,
                    onSetAppointmentLecturer = onSetAppointmentLecturer
                )
            } else {
                VerwaltungMissingDetailScreen(
                    title = "Kurs nicht gefunden",
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(
            route = VerwaltungDestination.UserDetail.route,
            arguments = listOf(
                navArgument(VerwaltungDestination.COURSE_ID_ARGUMENT) { type = NavType.StringType },
                navArgument(VerwaltungDestination.STUDENT_ID_ARGUMENT) { type = NavType.StringType }
            )
        ) { entry ->
            val courseId = entry.arguments?.getString(VerwaltungDestination.COURSE_ID_ARGUMENT).orEmpty()
            val studentId = entry.arguments?.getString(VerwaltungDestination.STUDENT_ID_ARGUMENT).orEmpty()
            val courseDetail = uiState.verwaltungCourseDetails[courseId]
            val userDetail = courseDetail?.toVerwaltungUserDetail(studentId)

            if (courseDetail != null && userDetail != null) {
                VerwaltungUserDetailScreen(
                    modifier = Modifier,
                    course = courseDetail,
                    userDetail = userDetail,
                    onBack = { navController.popBackStack() },
                    onOpenAttendances = onOpenAttendances,
                    onCreateActivationCode = onCreateActivationCode,
                    onRevokeDevice = onRevokeDevice,
                    onSetStudentModule = onSetStudentModule
                )
            } else {
                VerwaltungMissingDetailScreen(
                    title = "Nutzer nicht gefunden",
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }

    VerwaltungAttendanceSheets(
        uiState = uiState,
        onOpenAttendanceEdit = onOpenAttendanceEdit,
        onUpdateAttendance = onUpdateAttendance,
        onValidateAttendances = onValidateAttendances,
        onCloseSheet = onCloseSheet
    )
}

@Composable
private fun VerwaltungAttendanceSheets(
    uiState: HomeUiState,
    onOpenAttendanceEdit: () -> Unit,
    onUpdateAttendance: (UUID, String, AttendanceStatus) -> Unit,
    onValidateAttendances: (UUID) -> Unit,
    onCloseSheet: () -> Unit
) {
    val selectedAttendanceDay = uiState.verwaltungAttendanceDay

    when (uiState.activeSheet) {
        HomeSheet.VerwaltungAttendances -> {
            selectedAttendanceDay?.let {
                VerwaltungAttendanceSheet(
                    title = "Anwesenheiten",
                    attendanceDay = it,
                    onClose = onCloseSheet,
                    onConfirm = { onValidateAttendances(it.appointment.id) },
                    headerAction = {
                        IconButton(onClick = onOpenAttendanceEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Bearbeiten")
                        }
                    },
                    rowContent = { attendance ->
                        VerwaltungAttendanceStatusIcon(status = attendance.status)
                    }
                )
            }
        }

        HomeSheet.VerwaltungAttendanceEditor -> {
            selectedAttendanceDay?.let {
                val editorDay = it.copy(
                    attendances = it.attendances.map { attendance ->
                        attendance.copy(
                            status = uiState.attendanceEditorStatusOverrides[attendance.studentId]
                                ?: uiState.attendanceEditorDisplayedStatusOverrides[attendance.studentId]
                                ?: attendance.status
                        )
                    }
                )
                VerwaltungAttendanceSheet(
                    title = "Anwesenheiten bearbeiten",
                    attendanceDay = editorDay,
                    onClose = onCloseSheet,
                    onConfirm = { onValidateAttendances(it.appointment.id) },
                    headerAction = {
                        IconButton(onClick = { onValidateAttendances(it.appointment.id) }) {
                            Icon(Icons.Default.Check, contentDescription = "Speichern")
                        }
                    },
                    rowContent = { attendance ->
                        val isPending = attendance.studentId in uiState.pendingAttendanceStudentIds
                        VerwaltungAttendanceDropdown(
                            status = attendance.status,
                            loading = isPending,
                            onStatusChange = { selectedStatus ->
                                onUpdateAttendance(it.appointment.id, attendance.studentId, selectedStatus)
                            }
                        )
                    }
                )
            }
        }

        else -> Unit
    }
}

@Composable
private fun VerwaltungMissingDetailScreen(
    title: String,
    onBack: () -> Unit
) {
    VerwaltungDetailScaffold(
        title = title,
        onBack = onBack
    ) {
        Text(text = title)
    }
}

private sealed class VerwaltungDestination(val route: String) {
    data object CourseList : VerwaltungDestination("verwaltung/courses")
    data object CourseDetail :
        VerwaltungDestination("verwaltung/courses/{$COURSE_ID_ARGUMENT}") {
        fun createRoute(courseId: String): String = "verwaltung/courses/${Uri.encode(courseId)}"
    }

    data object UserDetail :
        VerwaltungDestination("verwaltung/courses/{$COURSE_ID_ARGUMENT}/users/{$STUDENT_ID_ARGUMENT}") {
        fun createRoute(courseId: String, studentId: String): String =
            "verwaltung/courses/${Uri.encode(courseId)}/users/${Uri.encode(studentId)}"
    }

    companion object {
        const val COURSE_ID_ARGUMENT = "courseId"
        const val STUDENT_ID_ARGUMENT = "studentId"
    }
}

private fun VerwaltungCourseDetail.toVerwaltungUserDetail(studentId: String?): VerwaltungUserDetail? {
    if (studentId == null) {
        return null
    }

    val user = users.firstOrNull { it.studentId == studentId } ?: return null
    return VerwaltungUserDetail(
        user = user,
        availableModules = availableModules,
        assignedModuleIds = moduleEnrollments[user.studentId].orEmpty(),
        attendanceDays = attendanceDays.mapNotNull { day ->
            val attendance = day.attendances.firstOrNull { it.studentId == studentId }
                ?: return@mapNotNull null

            VerwaltungUserAttendanceDay(
                appointment = day.appointment,
                status = attendance.status,
                lateMinutes = if (attendance.status == AttendanceStatus.Late) 10 else null
            )
        }
    )
}

@Preview
@Composable
private fun VerwaltungHomeScreenPreview() {
    AppTheme {
        VerwaltungHomeScreen(
            uiState = previewVerwaltungState(),
            onOpenSettings = {},
            onSearchQueryChange = {},
            onOpenAttendances = { _, _ -> },
            onOpenAttendanceEdit = {},
            onUpdateAttendance = { _, _, _ -> },
            onValidateAttendances = {},
            onCreateActivationCode = {},
            onRevokeDevice = {},
            onSetStudentModule = { _, _, _ -> },
            onSetAppointmentIgnored = { _, _ -> },
            onSetModuleIgnored = { _, _ -> },
            onReviewAbsence = { _, _ -> },
            onLoadBackendData = {},
            onRefreshCalendar = {},
            onCloseSheet = {}
        )
    }
}

private fun previewVerwaltungState(): HomeUiState {
    val courseDetail = previewCourseDetail()

    return HomeUiState(
        verwaltungCourses = listOf(
            VerwaltungCourse(id = "AI-WS24-IV", name = "AI-WS24-IV"),
            VerwaltungCourse(id = "AI-WS24-II", name = "AI-WS24-II"),
            VerwaltungCourse(id = "AI-WS22-III", name = "AI-WS22-III")
        ),
        verwaltungCourseDetails = mapOf(courseDetail.course.id to courseDetail)
    )
}

private fun previewCourseDetail(): VerwaltungCourseDetail {
    val appointments = previewAppointments()

    return VerwaltungCourseDetail(
        course = VerwaltungCourse(
            id = "AI-WS24-IV",
            name = "AI-WS24-IV"
        ),
        users = previewUsers(),
        appointments = appointments,
        attendanceDays = appointments.mapIndexed { index, appointment ->
            VerwaltungAttendanceDay(
                appointment = appointment,
                attendances = previewAttendances(index),
                attendancesValidated = index != 0
            )
        },
        availableModules = listOf(
            VerwaltungModule(id = "m1", courseGroupId = "AI-WS24-IV", name = "Software Engineering"),
            VerwaltungModule(id = "m2", courseGroupId = "AI-WS24-IV", name = "Datenbanken")
        )
    )
}

private fun previewAppointments(): List<Appointment> {
    val now = Instant.now()

    return listOf(
        Appointment(
            title = "Einführung in die Informatik",
            room = "203",
            startAt = now.minusSeconds(30 * 60L),
            endAt = now.plusSeconds(90 * 60L),
            course = "AI-WS24-IV",
            protokollant = "Max Mustermann"
        ),
        Appointment(
            title = "Datenbanken & Architekturen",
            room = "102",
            startAt = now.plusSeconds(3 * 60 * 60L),
            endAt = now.plusSeconds(6 * 60 * 60L),
            course = "AI-WS24-IV",
            protokollant = "Maja Müller"
        )
    )
}

private fun previewUsers(): List<VerwaltungUser> =
    listOf(
        VerwaltungUser(
            studentId = "123456",
            displayName = "Max Mustermann",
            activatedSince = Instant.now().minusSeconds(7 * 24 * 60 * 60L),
            generatedActivationCode = null
        ),
        VerwaltungUser(
            studentId = "123457",
            displayName = "Aylin Demir",
            activatedSince = Instant.now().minusSeconds(4 * 24 * 60 * 60L),
            generatedActivationCode = "BA-123457"
        )
    )

private fun previewAttendances(offset: Int): List<StudentAttendance> =
    previewUsers().mapIndexed { index, user ->
        StudentAttendance(
            studentId = user.studentId,
            displayName = user.displayName,
            status = if ((index + offset) % 2 == 0) AttendanceStatus.Present else AttendanceStatus.Absent
        )
    }
