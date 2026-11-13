package de.jscoding.bacheckin.feature.home.verwaltung

import android.content.ClipData
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import de.jscoding.bacheckin.core.ui.components.BCICard
import de.jscoding.bacheckin.core.ui.components.BCIList
import de.jscoding.bacheckin.core.ui.components.BCIListEntry
import de.jscoding.bacheckin.core.ui.components.BCISection
import de.jscoding.bacheckin.core.ui.theme.AppTheme
import de.jscoding.bacheckin.core.ui.theme.LocalCustomColors
import de.jscoding.bacheckin.feature.home.Appointment
import de.jscoding.bacheckin.feature.home.AttendanceStatus
import de.jscoding.bacheckin.feature.home.StudentAttendance
import de.jscoding.bacheckin.feature.home.VerwaltungAttendanceDay
import de.jscoding.bacheckin.feature.home.VerwaltungCourse
import de.jscoding.bacheckin.feature.home.VerwaltungCourseDetail
import de.jscoding.bacheckin.feature.home.VerwaltungUser
import de.jscoding.bacheckin.feature.home.VerwaltungUserAttendanceDay
import de.jscoding.bacheckin.shared.model.BackendDtos
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerwaltungDetailTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Zurück"
                )
            }
        },
        actions = actions
    )
}

@Composable
fun VerwaltungDetailScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            VerwaltungDetailTopBar(title = title, onBack = onBack)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            content()
        }
    }
}

@Composable
fun VerwaltungUserManagementCard(
    user: VerwaltungUser,
    onCreateActivationCode: (String) -> Unit,
    onRevokeDevice: (String) -> Unit
) {
    val hasActiveDevice = user.activatedSince != null
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BCICard {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Freigeschaltet seit",
                    style = typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = user.activatedSince?.verwaltungDateTimeText()
                        ?: "Kein Gerät freigeschaltet",
                    style = typography.headlineSmall
                )
                user.generatedActivationCode?.let { activationCode ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SelectionContainer {
                            Text(
                                text = "Freischaltcode: $activationCode",
                                style = typography.bodyLarge,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            coroutineScope.launch {
                                clipboard.setClipEntry(
                                    ClipEntry(
                                        ClipData.newPlainText(
                                            "Freischaltcode",
                                            activationCode
                                        )
                                    )
                                )
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Freischaltcode kopieren"
                            )
                        }
                    }

                    val qrCodeBitmap = remember(activationCode) {
                        val qrCodeText = "ba-checkin://activation?code=$activationCode"
                        generateQRCode(qrCodeText)
                    }
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Image(
                            bitmap = qrCodeBitmap.asImageBitmap(),
                            contentDescription = "QR-Code für Freischaltcode",
                            modifier = Modifier.size(256.dp)
                        )
                    }
                }
                Button(
                    onClick = { onCreateActivationCode(user.studentId) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Key, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Freischaltcode generieren")
                }
            }
        }

        OutlinedButton(
            onClick = { onRevokeDevice(user.studentId) },
            enabled = hasActiveDevice,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = colorScheme.error
            )
        ) {
            Icon(Icons.Outlined.Lock, contentDescription = null)
            Text(if (hasActiveDevice) "Gerät sperren" else "Kein Gerät aktiv")
        }
    }
}

@Composable
fun VerwaltungAttendanceDayRow(
    day: VerwaltungAttendanceDay,
    onClick: () -> Unit
) {
    val presentCount = day.attendances.count {
        it.status == AttendanceStatus.Present || it.status == AttendanceStatus.Late
    }

    BCIListEntry.DefaultRow(
        title = day.appointment.verwaltungDateText(),
        subtitle = day.appointment.title,
        onClick = onClick,
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("$presentCount/${day.attendances.size}")
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
fun VerwaltungUserAttendanceDayRow(
    day: VerwaltungUserAttendanceDay,
    onClick: () -> Unit
) {
    BCIListEntry.DefaultRow(
        title = day.appointment.verwaltungDateText(),
        subtitle = day.appointment.title,
        onClick = onClick,
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                day.lateMinutes?.let {
                    Text(
                        text = "$it min",
                        color = LocalCustomColors.current.warning,
                        style = typography.labelLarge
                    )
                }
                VerwaltungAttendanceStatusIcon(status = day.status)
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
fun VerwaltungAppointmentListCard(appointments: List<Appointment>) {
    BCIList(
        listEntries = appointments.map { appointment ->
            {
                BCIListEntry.DefaultRow(
                    title = appointment.title,
                    subtitle = "${appointment.room} - ${appointment.verwaltungDateText()} - ${appointment.verwaltungTimeText()}"
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerwaltungAppointmentSheet(
    appointment: Appointment,
    moduleName: String?,
    moduleIgnored: Boolean,
    onClose: () -> Unit,
    onIgnoredChange: (Boolean) -> Unit,
    onModuleIgnoredChange: (Boolean) -> Unit,
    lecturers: List<BackendDtos.Lecturer> = emptyList(),
    moduleLecturerId: String? = null,
    isLoading: Boolean = false,
    onModuleLecturerChange: (String?) -> Unit = {},
    onAppointmentLecturerChange: (Boolean, String?) -> Unit = { _, _ -> }
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()
    val scrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                return if (available.y < 0f) available else Offset.Zero
            }
        }
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onClose,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Schließen")
            }
            Text(
                text = "Termindetails",
                style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .nestedScroll(scrollConnection)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 32.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = appointment.title,
                style = typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            BCISection(title = "Zeit") {
                Text("${appointment.verwaltungDateText()} · ${appointment.verwaltungTimeText()}")
            }
            BCISection(title = "Ort") {
                Text(appointment.room.ifBlank { "Kein Raum angegeben" })
            }
            BCISection(title = "Kurs") {
                Text(appointment.course.ifBlank { "Kein Kurs angegeben" })
            }
            BCISection(title = "Modul") {
                Text(moduleName ?: "Kein Modul zugeordnet")
            }
            BCISection(title = "Dozent für dieses Modul") {
                VerwaltungLecturerDropdown(
                    lecturers = lecturers,
                    lecturerId = moduleLecturerId,
                    enabled = moduleName != null && !isLoading,
                    onSelect = onModuleLecturerChange
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = appointment.lecturerOverride,
                    enabled = appointment.lecturerEditable && !isLoading,
                    onCheckedChange = { checked -> onAppointmentLecturerChange(checked, null) }
                )
                Text("Anderer Dozent")
            }
            if (appointment.lecturerOverride) {
                VerwaltungLecturerDropdown(
                    lecturers = lecturers,
                    lecturerId = appointment.lecturerId,
                    enabled = appointment.lecturerEditable && !isLoading,
                    onSelect = { onAppointmentLecturerChange(true, it) }
                )
            }
            BCISection(title = "Protokollant") {
                Text(appointment.protokollant.ifBlank { "Nicht festgelegt" })
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = appointment.ignored,
                    onCheckedChange = onIgnoredChange
                )
                Text("Termin ignorieren")
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = moduleIgnored,
                    onCheckedChange = onModuleIgnoredChange,
                    enabled = moduleName != null
                )
                Text("Modul ignorieren")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VerwaltungLecturerDropdown(
    lecturers: List<BackendDtos.Lecturer>,
    lecturerId: String?,
    enabled: Boolean,
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = lecturers.firstOrNull { it.id == lecturerId }
    val displayText = selected?.let { it.displayName + if (it.active) "" else " (inaktiv)" } ?: "Kein Dozent zugewiesen"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
                .fillMaxWidth(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Keine Zuordnung") },
                onClick = {
                    expanded = false
                    onSelect(null)
                }
            )
            lecturers.filter { it.active }.forEach { lecturer ->
                DropdownMenuItem(
                    text = { Text(lecturer.displayName) },
                    onClick = {
                        expanded = false
                        onSelect(lecturer.id)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerwaltungAttendanceSheet(
    title: String,
    attendanceDay: VerwaltungAttendanceDay,
    onClose: () -> Unit,
    onConfirm: () -> Unit,
    headerAction: @Composable () -> Unit,
    rowContent: @Composable (StudentAttendance) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()
    val scrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                return if (available.y < 0f) available else Offset.Zero
            }
        }
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onClose,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Schließen")
            }
            Text(
                text = title,
                style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )
            headerAction()
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .nestedScroll(scrollConnection)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 32.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(
                text = "${attendanceDay.appointment.title} · ${attendanceDay.appointment.verwaltungTimeText()}",
                style = typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )

            defaultVerwaltungAttendanceGroups(attendanceDay.attendances).forEach { (sectionTitle, attendances) ->
                BCISection(title = sectionTitle) {
                    BCIList(
                        listEntries = attendances.map { attendance ->
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

            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Text("Anwesenheiten validieren")
            }
        }
    }
}

@Composable
fun VerwaltungAttendanceStatusIcon(status: AttendanceStatus) {
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
        tint = tint
    )
}

@Composable
fun VerwaltungAttendanceDropdown(
    status: AttendanceStatus,
    loading: Boolean,
    onStatusChange: (AttendanceStatus) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        }
        TextButton(
            enabled = !loading,
            onClick = { expanded = true }
        ) {
            Text(status.displayName())
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AttendanceStatus.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayName()) },
                    leadingIcon = { VerwaltungAttendanceStatusIcon(status = option) },
                    onClick = {
                        expanded = false
                        if (option != status) {
                            onStatusChange(option)
                        }
                    }
                )
            }
        }
    }
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

fun List<VerwaltungCourse>.filterBy(
    query: String,
    courseDetails: Map<String, VerwaltungCourseDetail>
): List<VerwaltungCourse> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isBlank()) {
        return this
    }

    return filter { course ->
        val hasMatchingUser = courseDetails[course.id]
            ?.users
            ?.any { user ->
                user.displayName.contains(normalizedQuery, ignoreCase = true) ||
                        user.studentId.contains(normalizedQuery, ignoreCase = true)
            } == true

        course.name.contains(normalizedQuery, ignoreCase = true) ||
                course.id.contains(normalizedQuery, ignoreCase = true) ||
                hasMatchingUser
    }
}

enum class VerwaltungCourseTab {
    Verwaltung,
    Anwesenheiten,
    Termine
}

enum class VerwaltungUserTab {
    Nutzerverwaltung,
    Module,
    Anwesenheiten
}

private fun defaultVerwaltungAttendanceGroups(
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

val VerwaltungCourseTab.label: String
    get() = when (this) {
        VerwaltungCourseTab.Verwaltung -> "Verwaltung"
        VerwaltungCourseTab.Anwesenheiten -> "Anwesenheit"
        VerwaltungCourseTab.Termine -> "Termine"
    }

val VerwaltungUserTab.label: String
    get() = when (this) {
        VerwaltungUserTab.Nutzerverwaltung -> "Verwaltung"
        VerwaltungUserTab.Module -> "Module"
        VerwaltungUserTab.Anwesenheiten -> "Anwesenheit"
    }

fun Appointment.verwaltungDateText(): String =
    DateTimeFormatter.ofPattern("dd.MM.yyyy")
        .withZone(ZoneId.systemDefault())
        .format(startAt)

fun Appointment.verwaltungTimeText(): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    return "${formatter.format(startAt)}-${formatter.format(endAt)} Uhr"
}

private fun Instant.verwaltungDateTimeText(): String =
    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(this)

fun generateQRCode(
    text: String,
    width: Int = 512,
    height: Int = 512
): Bitmap {
    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height)
    val bitmap = createBitmap(width, height, Bitmap.Config.RGB_565)
    for (x in 0 until width) {
        for (y in 0 until height) {
            bitmap[x, y] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
        }
    }
    return bitmap
}

@Preview
@Composable
private fun VerwaltungUserManagementCardPreview() {
    AppTheme {
        VerwaltungUserManagementCard(
            user = VerwaltungUser(
                studentId = "123456",
                displayName = "Max Mustermann",
                activatedSince = Instant.now(),
                generatedActivationCode = "BA-123456"
            ),
            onCreateActivationCode = {},
            onRevokeDevice = {}
        )
    }
}

@Preview
@Composable
private fun VerwaltungAttendanceDayRowPreview() {
    AppTheme {
        val now = Instant.now()
        val appointment = Appointment(
            title = "Einführung in die Informatik",
            room = "203",
            startAt = now,
            endAt = now.plusSeconds(3600),
            course = "AI-WS24-IV",
            protokollant = "Max Mustermann"
        )
        VerwaltungAttendanceDayRow(
            day = VerwaltungAttendanceDay(
                appointment = appointment,
                attendances = listOf(
                    StudentAttendance("1", "Max Mustermann", AttendanceStatus.Present),
                    StudentAttendance("2", "Aylin Demir", AttendanceStatus.Absent)
                ),
                attendancesValidated = true
            ),
            onClick = {}
        )
    }
}

@Preview
@Composable
private fun VerwaltungUserAttendanceDayRowPreview() {
    AppTheme {
        val now = Instant.now()
        val appointment = Appointment(
            title = "Datenbanken & Architekturen",
            room = "102",
            startAt = now,
            endAt = now.plusSeconds(3600),
            course = "AI-WS24-IV",
            protokollant = "Maja Müller"
        )
        VerwaltungUserAttendanceDayRow(
            day = VerwaltungUserAttendanceDay(
                appointment = appointment,
                status = AttendanceStatus.Late,
                lateMinutes = 15
            ),
            onClick = {}
        )
    }
}

@Preview
@Composable
private fun VerwaltungAppointmentListCardPreview() {
    AppTheme {
        val now = Instant.now()
        VerwaltungAppointmentListCard(
            appointments = listOf(
                Appointment(
                    title = "Einführung in die Informatik",
                    room = "203",
                    startAt = now,
                    endAt = now.plusSeconds(3600),
                    course = "AI-WS24-IV",
                    protokollant = "Max Mustermann"
                ),
                Appointment(
                    title = "Datenbanken & Architekturen",
                    room = "102",
                    startAt = now.plusSeconds(7200),
                    endAt = now.plusSeconds(10800),
                    course = "AI-WS24-IV",
                    protokollant = "Maja Müller"
                )
            )
        )
    }
}

@Preview
@Composable
private fun VerwaltungAttendanceStatusIconPreview() {
    AppTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AttendanceStatus.entries.forEach { status ->
                VerwaltungAttendanceStatusIcon(status = status)
            }
        }
    }
}
