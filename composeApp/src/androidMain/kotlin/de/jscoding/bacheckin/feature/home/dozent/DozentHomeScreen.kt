package de.jscoding.bacheckin.feature.home.dozent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.jscoding.bacheckin.core.ui.components.BCIDivider
import de.jscoding.bacheckin.core.ui.components.BCIList
import de.jscoding.bacheckin.core.ui.components.BCIListEntry
import de.jscoding.bacheckin.core.ui.components.BCISection
import de.jscoding.bacheckin.core.ui.components.BCIWelcomeBar
import de.jscoding.bacheckin.core.ui.theme.AppTheme
import de.jscoding.bacheckin.core.ui.theme.LocalCustomColors
import de.jscoding.bacheckin.feature.home.Appointment
import de.jscoding.bacheckin.feature.home.AttendanceStatus
import de.jscoding.bacheckin.feature.home.Day
import de.jscoding.bacheckin.feature.home.HomeSheet
import de.jscoding.bacheckin.feature.home.HomeUiState
import de.jscoding.bacheckin.feature.home.StudentAttendance
import de.jscoding.bacheckin.feature.home.components.HomeAppointmentCard
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DozentHomeScreen(
    modifier: Modifier = Modifier,
    uiState: HomeUiState,
    onOpenSettings: () -> Unit,
    onSelectDay: (Day) -> Unit,
    onCloseSheet: () -> Unit,
    onAcceptAppointment: () -> Unit,
    onOpenAppointment: (UUID) -> Unit,
    onSelectProtokollant: (UUID, String) -> Unit,
    onOpenAttendanceEdit: () -> Unit,
    onUpdateStudentAttendance: (UUID, String, AttendanceStatus) -> Unit,
    onValidateAttendances: (UUID) -> Unit,
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
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    val activeAppointment = uiState.activeAppointment
                    if (activeAppointment != null) {
                        ActiveAppointmentCard(
                            appointment = activeAppointment,
                            studentCount = uiState.studentCount,
                            studentAttendCount = uiState.studentAttendCount,
                            attendancesValidated = uiState.attendancesValidated,
                            protokollantCandidates = uiState.attendances,
                            onSelectProtokollant = { studentId ->
                                onSelectProtokollant(activeAppointment.id, studentId)
                            },
                            onAccept = onAcceptAppointment,
                            onOpenAppointment = { onOpenAppointment(activeAppointment.id) }
                        )
                    } else {
                        InactiveAppointmentCard()
                    }
                    Spacer(Modifier.size(64.dp))
                }
            }

            item {
                HomeAppointmentCard(
                    appointments = uiState.appointments,
                    selectedDay = uiState.selectedDay,
                    onSelectDay = onSelectDay
                )
            }
        }
    }

    val activeAppointment = uiState.activeAppointment
    when (uiState.activeSheet) {
        HomeSheet.Attendances -> {
            if (activeAppointment != null) {
                AttendancesSheet(
                    appointment = activeAppointment,
                    attendances = uiState.attendances,
                    onClose = onCloseSheet,
                    onConfirm = onAcceptAppointment,
                    onEdit = onOpenAttendanceEdit
                )
            }
        }

        HomeSheet.AttendanceEditor -> {
            if (activeAppointment != null) {
                AttendancesEditSheet(
                    appointment = activeAppointment,
                    attendances = uiState.attendances,
                    statusOverrides = uiState.attendanceEditorStatusOverrides,
                    displayedStatusOverrides = uiState.attendanceEditorDisplayedStatusOverrides,
                    pendingStudentIds = uiState.pendingAttendanceStudentIds,
                    showConfirmAction = false,
                    onClose = onCloseSheet,
                    onConfirm = {},
                    onAttendanceStatusChange = { studentId, status ->
                        onUpdateStudentAttendance(activeAppointment.id, studentId, status)
                    }
                )
            }
        }

        HomeSheet.AttendanceConfirmationEditor -> {
            if (activeAppointment != null) {
                AttendancesEditSheet(
                    appointment = activeAppointment,
                    attendances = uiState.attendances,
                    statusOverrides = uiState.attendanceEditorStatusOverrides,
                    displayedStatusOverrides = uiState.attendanceEditorDisplayedStatusOverrides,
                    pendingStudentIds = uiState.pendingAttendanceStudentIds,
                    showConfirmAction = true,
                    onClose = onCloseSheet,
                    onConfirm = { onValidateAttendances(activeAppointment.id) },
                    onAttendanceStatusChange = { studentId, status ->
                        onUpdateStudentAttendance(activeAppointment.id, studentId, status)
                    }
                )
            }
        }

        HomeSheet.Absence,
        HomeSheet.VerwaltungAttendances,
        HomeSheet.VerwaltungAttendanceEditor,
        null -> Unit
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ActiveAppointmentCard(
    modifier: Modifier = Modifier,
    appointment: Appointment,
    studentCount: Int,
    studentAttendCount: Int,
    attendancesValidated: Boolean,
    protokollantCandidates: List<StudentAttendance>,
    onSelectProtokollant: (String) -> Unit,
    onAccept: () -> Unit,
    onOpenAppointment: () -> Unit
) {
    var showProtokollantDialog by remember { mutableStateOf(false) }

    BCIList(
        modifier = modifier.clickable(onClick = onOpenAppointment),
        listEntries = listOf(
            {
                Column {
                    Text(
                        text = "Meine aktuellen Vorlesungen",
                        style = typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.padding(4.dp))
                    Text(text = appointment.course, style = typography.bodyLarge)
                    Spacer(Modifier.padding(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = appointment.timeText(),
                            style = typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontStyle = FontStyle.Italic
                            )
                        )

                        Spacer(Modifier.weight(1f))

                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null
                        )
                        Text(
                            text = appointment.room,
                            style = typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontStyle = FontStyle.Italic
                            ),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    Spacer(Modifier.padding(4.dp))
                }
            },
            {
                BCIListEntry.DefaultRow(
                    title = "Protokollant",
                    subtitle = appointment.protokollant,
                    verticalPadding = 2.dp,
                    trailingContent = {
                        TextButton(
                            onClick = { showProtokollantDialog = true },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "Auswählen",
                                style = typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Icon(
                                imageVector = Icons.Default.UnfoldMore,
                                contentDescription = null,
                            )
                        }
                    }
                )
            },
            {
                Column {
                    Text(
                        modifier = Modifier.padding(vertical = 8.dp),
                        text = buildAnnotatedString {
                            append("Es sind ")
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(studentAttendCount.toString())
                            }
                            append(" von ")
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(studentCount.toString())
                            }
                            append(" Studierenden anwesend")
                        }
                    )

                    if (attendancesValidated) {
                        Text(
                            text = "Anwesenheiten validiert",
                            style = typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = LocalCustomColors.current.success
                        )
                    }

                    Spacer(Modifier.padding(8.dp))

                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LocalCustomColors.current.successContainer,
                            contentColor = LocalCustomColors.current.onSuccessContainer,
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Bestätigen",
                            style = typography.bodyLarge
                        )
                    }
                }
            }
        )
    )

    if (showProtokollantDialog) {
        ProtokollantPickerDialog(
            candidates = protokollantCandidates,
            currentProtokollant = appointment.protokollant,
            onDismiss = { showProtokollantDialog = false },
            onSelect = { studentId ->
                onSelectProtokollant(studentId)
                showProtokollantDialog = false
            }
        )
    }
}

@Composable
private fun InactiveAppointmentCard(
    modifier: Modifier = Modifier,
) {
    BCIList(
        modifier = modifier,
        listEntries = listOf(
            {
                Text(
                    text = "Meine aktuellen Vorlesungen",
                    style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            },
            {
                Text(
                    text = "Du hast aktuell nichts zu tun.",
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                )
            }
        )
    )
}

@Composable
private fun ProtokollantPickerDialog(
    candidates: List<StudentAttendance>,
    currentProtokollant: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCandidates = candidates
        .distinctBy { it.studentId }
        .filter {
            val query = searchQuery.trim()
            query.isBlank() ||
                    it.displayName.contains(query, ignoreCase = true) ||
                    it.studentId.contains(query, ignoreCase = true)
        }
        .sortedBy { it.displayName }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Protokollant auswählen") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Nach Namen suchen") }
                )

                if (filteredCandidates.isEmpty()) {
                    Text(
                        text = "Keine Person gefunden.",
                        style = typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 320.dp)
                    ) {
                        items(
                            count = filteredCandidates.size,
                            key = { index -> filteredCandidates[index].studentId }
                        ) { index ->
                            val candidate = filteredCandidates[index]
                            val isSelected = candidate.displayName == currentProtokollant

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(candidate.studentId) }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = candidate.displayName,
                                        style = typography.bodyLarge.copy(
                                            fontWeight = if (isSelected) {
                                                FontWeight.SemiBold
                                            } else {
                                                FontWeight.Normal
                                            }
                                        )
                                    )
                                    Text(
                                        text = candidate.studentId,
                                        style = typography.labelSmall,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = LocalCustomColors.current.success
                                    )
                                }
                            }

                            if (index < filteredCandidates.lastIndex) {
                                BCIDivider()
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Schließen")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttendancesSheet(
    appointment: Appointment,
    attendances: List<StudentAttendance>,
    onClose: () -> Unit,
    onConfirm: () -> Unit,
    onEdit: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onClose,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = colorScheme.surface
    ) {
        AttendanceSheetContent(
            title = "Anwesenheiten",
            appointment = appointment,
            attendances = attendances,
            onClose = onClose,
            onConfirm = onConfirm,
            showConfirmAction = false,
            headerAction = {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Bearbeiten")
                }
            },
            rowContent = { attendance ->
                AttendanceStatusIcon(status = attendance.status)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttendancesEditSheet(
    appointment: Appointment,
    attendances: List<StudentAttendance>,
    statusOverrides: Map<String, AttendanceStatus>,
    displayedStatusOverrides: Map<String, AttendanceStatus>,
    pendingStudentIds: Set<String>,
    showConfirmAction: Boolean,
    onClose: () -> Unit,
    onConfirm: () -> Unit,
    onAttendanceStatusChange: (String, AttendanceStatus) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onClose,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = colorScheme.surface
    ) {
        AttendanceSheetContent(
            title = "Anwesenheiten bearbeiten",
            appointment = appointment,
            attendances = attendances,
            groupedAttendances = attendanceEditorGroups(attendances, displayedStatusOverrides),
            onClose = onClose,
            onConfirm = onConfirm,
            showConfirmAction = showConfirmAction,
            headerAction = {},
            rowContent = { attendance ->
                val status = statusOverrides[attendance.studentId] ?: attendance.status
                val isPending = attendance.studentId in pendingStudentIds
                AttendanceSwitch(
                    status = status,
                    loading = isPending,
                    onStatusChange = { nextStatus ->
                        onAttendanceStatusChange(attendance.studentId, nextStatus)
                    },
                )
            }
        )
    }
}

@Composable
private fun AttendanceSheetContent(
    title: String,
    appointment: Appointment,
    attendances: List<StudentAttendance>,
    groupedAttendances: List<Pair<String, List<StudentAttendance>>> =
        defaultAttendanceGroups(attendances),
    onClose: () -> Unit,
    onConfirm: () -> Unit,
    showConfirmAction: Boolean = true,
    headerAction: @Composable () -> Unit,
    rowContent: @Composable (StudentAttendance) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        AttendanceSheetHeader(
            title = title,
            onClose = onClose,
            onConfirm = onConfirm,
            showConfirmAction = showConfirmAction,
            headerAction = headerAction
        )

        Text(
            text = "${appointment.title} • ${appointment.timeText()}",
            style = typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp, bottom = 20.dp)
        )

        groupedAttendances.forEach { (sectionTitle, sectionAttendances) ->
            BCISection(
                title = sectionTitle,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            ) {
                BCIList(
                    listEntries = sectionAttendances.map { attendance ->
                        {
                            BCIListEntry.DefaultRow(
                                title = attendance.displayName,
                                subtitle = attendance.studentId,
                                trailingContent = { rowContent(attendance) }
                            )
                        }
                    },
                    innerPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                )
            }
        }
    }
}

@Composable
private fun AttendanceSheetHeader(
    title: String,
    onClose: () -> Unit,
    onConfirm: () -> Unit,
    showConfirmAction: Boolean,
    headerAction: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Schließen",
                modifier = Modifier.size(32.dp),
                tint = colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = title,
            style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        )

        headerAction()

        if (showConfirmAction) {
            Spacer(Modifier.width(8.dp))

            FloatingActionButton(
                onClick = onConfirm,
                shape = CircleShape,
                containerColor = LocalCustomColors.current.success,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Anwesenheiten validieren",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

private fun defaultAttendanceGroups(
    attendances: List<StudentAttendance>
): List<Pair<String, List<StudentAttendance>>> =
    listOf(
        "Fehlend" to attendances.filter { it.status == AttendanceStatus.Missing },
        "Anwesend" to attendances.filter {
            it.status == AttendanceStatus.Present || it.status == AttendanceStatus.Late
        },
        "Teilweise" to attendances.filter { it.status == AttendanceStatus.Partially },
        "Abwesend" to attendances.filter { it.status == AttendanceStatus.Absent },
        "Entschuldigt" to attendances.filter { it.status == AttendanceStatus.Excused },
        "Prüfung offen" to attendances.filter { it.status == AttendanceStatus.ManualReview }
    )

private fun attendanceEditorGroups(
    attendances: List<StudentAttendance>,
    displayedStatuses: Map<String, AttendanceStatus>
): List<Pair<String, List<StudentAttendance>>> =
    defaultAttendanceGroups(
        attendances.map { attendance ->
            attendance.copy(
                status = displayedStatuses[attendance.studentId] ?: attendance.status
            )
        }
    )

@Composable
private fun AttendanceStatusIcon(status: AttendanceStatus) {
    val customColors = LocalCustomColors.current
    val (icon, tint) = when (status) {
        AttendanceStatus.Missing -> Icons.AutoMirrored.Outlined.HelpOutline to colorScheme.onSurface
        AttendanceStatus.Present -> Icons.Outlined.CheckCircle to customColors.success
        AttendanceStatus.Late -> Icons.Outlined.Schedule to customColors.warning
        AttendanceStatus.Partially -> Icons.Outlined.Schedule to customColors.warning
        AttendanceStatus.Absent -> Icons.Outlined.Cancel to colorScheme.error
        AttendanceStatus.Excused -> Icons.Outlined.CheckCircle to customColors.success
        AttendanceStatus.ManualReview -> Icons.AutoMirrored.Outlined.HelpOutline to customColors.warning
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(30.dp)
    )
}

@Composable
private fun AttendanceSwitch(
    status: AttendanceStatus,
    loading: Boolean = false,
    onStatusChange: (AttendanceStatus) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = colorScheme.primary
            )
        } else {
            Spacer(Modifier.size(24.dp))
        }

        TextButton(
            enabled = !loading,
            onClick = { onStatusChange(status.nextPersistentStatus()) }
        ) {
            Text(status.displayName())
        }
    }
}

private fun AttendanceStatus.nextPersistentStatus(): AttendanceStatus {
    val values = listOf(AttendanceStatus.Present, AttendanceStatus.Late, AttendanceStatus.Partially,
        AttendanceStatus.Absent, AttendanceStatus.Excused, AttendanceStatus.ManualReview)
    return values[(values.indexOf(this).coerceAtLeast(-1) + 1) % values.size]
}

private fun AttendanceStatus.displayName() = when (this) {
    AttendanceStatus.Missing -> "Fehlend"
    AttendanceStatus.Present -> "Anwesend"
    AttendanceStatus.Late -> "Verspätet"
    AttendanceStatus.Partially -> "Teilweise"
    AttendanceStatus.Absent -> "Abwesend"
    AttendanceStatus.Excused -> "Entschuldigt"
    AttendanceStatus.ManualReview -> "Prüfung offen"
}

@Preview
@Composable
private fun DozentHomeScreenPreview() {
    AppTheme {
        DozentHomeScreen(
            uiState = HomeUiState(
                appointments = listOf(
                    Appointment(
                        title = "Einführung in die Informatik",
                        room = "203",
                        startAt = Instant.now().minusSeconds(30 * 60L),
                        endAt = Instant.now().plusSeconds(90 * 60L),
                        course = "WS-24-IV",
                        protokollant = "Max Mustermann"
                    ),
                    Appointment(
                        title = "Datenbanken & Architekturen",
                        room = "102",
                        startAt = Instant.now().plusSeconds(3 * 60 * 60L),
                        endAt = Instant.now().plusSeconds(6 * 60 * 60L),
                        course = "WS-24-II",
                        protokollant = "Maja Müller"
                    )
                ),
                activeAppointment = Appointment(
                    title = "Einführung in die Informatik",
                    room = "203",
                    startAt = Instant.now().minusSeconds(30 * 60L),
                    endAt = Instant.now().plusSeconds(90 * 60L),
                    course = "WS-24-IV",
                    protokollant = "Max Mustermann"
                ),
                attendances = listOf(
                    StudentAttendance("123456", "Max Mustermann", AttendanceStatus.Missing),
                    StudentAttendance("123457", "Aylin Demir", AttendanceStatus.Missing),
                    StudentAttendance("123458", "Jonas Weber", AttendanceStatus.Present),
                    StudentAttendance("123459", "Maja Müller", AttendanceStatus.Late),
                    StudentAttendance("123460", "Noah Schneider", AttendanceStatus.Absent)
                ),
                studentCount = 5,
                studentAttendCount = 2,
                attendancesValidated = false
            ),
            onOpenSettings = { },
            onSelectDay = { },
            onCloseSheet = { },
            onOpenAppointment = {},
            onAcceptAppointment = {},
            onSelectProtokollant = { _, _ -> },
            onOpenAttendanceEdit = {},
            onUpdateStudentAttendance = { _, _, _ -> },
            onValidateAttendances = {}
        )
    }
}

private fun Appointment.timeText(): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    return "${formatter.format(startAt)}-${formatter.format(endAt)} Uhr"
}
