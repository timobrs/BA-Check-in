package de.jscoding.bacheckin.feature.home.student

import android.annotation.SuppressLint
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Contactless
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import de.jscoding.bacheckin.core.ui.components.BCICard
import de.jscoding.bacheckin.core.ui.components.BCIWelcomeBar
import de.jscoding.bacheckin.core.ui.theme.AppTheme
import de.jscoding.bacheckin.core.ui.theme.LocalCustomColors
import de.jscoding.bacheckin.feature.home.Appointment
import de.jscoding.bacheckin.feature.home.CheckInState
import de.jscoding.bacheckin.feature.home.Day
import de.jscoding.bacheckin.feature.home.HomeSheet
import de.jscoding.bacheckin.feature.home.HomeUiState
import de.jscoding.bacheckin.feature.home.ReportedAbsence
import de.jscoding.bacheckin.feature.home.components.HomeAppointmentCard
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(
    uiState: HomeUiState,
    onOpenSettings: () -> Unit,
    onSelectDay: (Day) -> Unit,
    onRetryCheckIn: () -> Unit,
    onOpenAbwesenheitSheet: () -> Unit,
    onReportAbsence: (Long, Long) -> Unit,
    onCloseSheet: () -> Unit,
    modifier: Modifier = Modifier,
    actionsBesideSettings: @Composable RowScope.() -> Unit = {}
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            BCIWelcomeBar(
                subtitle = buildString {
                    if (uiState.studentId != null) {
                        append("Matrikelnummer: ${uiState.studentId}")
                    }
                    if (!uiState.studentName.isNullOrBlank()) {
                        if (isNotEmpty()) append(", ")
                        append(uiState.studentName)
                    }
                }.ifEmpty { null },
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
                CheckInCard(uiState.checkInState, uiState.studentId, onRetryCheckIn)
            }

            item {
                Button(
                    onClick = onOpenAbwesenheitSheet,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primaryContainer,
                        contentColor = colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Abwesenheit melden")
                }
            }

            item {
                Spacer(Modifier.size(32.dp))
            }

            item {
                HomeAppointmentCard(
                    appointments = uiState.appointments,
                    selectedDay = uiState.selectedDay,
                    onSelectDay = onSelectDay
                )
            }

            item {
                AbsenceOverviewCard(absences = uiState.absences)
            }
        }
    }

    when (uiState.activeSheet) {
        HomeSheet.Absence -> {
            AbwesenheitSheet(
                onClose = onCloseSheet,
                onReport = onReportAbsence
            )
        }

        else -> Unit
    }
}

@Composable
private fun CheckInCard(state: CheckInState, studentId: String?, onRetry: () -> Unit) {
    val customColors = LocalCustomColors.current
    BCICard(
        modifier = Modifier
            .fillMaxWidth()
            .height(324.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val title = when (state) {
                is CheckInState.Absent -> "Abwesend"
                is CheckInState.Failed -> "Fehlgeschlagen"
                is CheckInState.Success -> "Eingecheckt"
                is CheckInState.Scanning -> "Check-in"
                is CheckInState.Waiting -> "Nächster Check-in"
                CheckInState.Unavailable -> "Check-in"
            }
            val subtitle = when (state) {
                is CheckInState.Absent -> if (state.isFullDay || state.untilAt == null) {
                    "Du bist für heute als abwesend eingetragen"
                } else {
                    "Abwesend bis ${state.untilAt.formatCheckInUntil()}"
                }
                is CheckInState.Failed -> "Der Check-in ist fehlgeschlagen."
                is CheckInState.Success -> "Dein Check-in ist aktiv"
                is CheckInState.Scanning -> "Scanne die Karte mit deinem Handy"
                is CheckInState.Waiting -> state.message
                CheckInState.Unavailable -> "Check-in derzeit nicht verfügbar"
            }

            Text(
                title,
                style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.align(Alignment.Start)
            )
            Text(
                subtitle,
                style = typography.bodyMedium,
                modifier = Modifier.align(Alignment.Start)
            )
            if (state is CheckInState.Success) {
                Text(
                    text = "Gültig bis ${state.untilAt.formatCheckInUntil()}",
                    style = typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = customColors.success,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(top = 4.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (state) {
                    is CheckInState.Absent -> {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint = colorScheme.error,
                            modifier = Modifier.size(120.dp)
                        )
                    }

                    is CheckInState.Failed -> {
                        ScannerFrame(
                            backgroundColor = colorScheme.errorContainer,
                            bracketColor = colorScheme.error.copy(alpha = 0.1f)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                tint = colorScheme.error,
                                modifier = Modifier.size(80.dp)
                            )
                        }
                    }

                    is CheckInState.Success -> {
                        ScannerFrame(
                            backgroundColor = customColors.successContainer,
                            bracketColor = customColors.success.copy(alpha = 0.1f)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = customColors.success,
                                modifier = Modifier.size(80.dp)
                            )
                        }
                    }

                    is CheckInState.Scanning -> {
                        ScannerFrame(
                            backgroundColor = Color.Transparent,
                            bracketColor = colorScheme.primary,
                            showScanLine = true
                        ) {
                            (studentId ?: state.nfcId)?.let { id ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.Contactless,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = customColors.success
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = id,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                    is CheckInState.Waiting -> {
                        Icon(
                            Icons.Outlined.Contactless,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(80.dp)
                        )
                    }
                    CheckInState.Unavailable -> {
                        Icon(
                            Icons.Outlined.Contactless,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(80.dp)
                        )
                    }
                }
            }

            if (state is CheckInState.Failed) {
                TextButton(onClick = onRetry) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Erneut versuchen")
                }
            }
        }
    }
}

private fun Instant.formatCheckInUntil(): String {
    val zoneId = ZoneId.systemDefault()
    val localDateTime = atZone(zoneId).toLocalDateTime()
    val pattern = if (localDateTime.toLocalDate() == LocalDate.now(zoneId)) {
        "HH:mm 'Uhr'"
    } else {
        "dd.MM.yyyy, HH:mm 'Uhr'"
    }
    return DateTimeFormatter.ofPattern(pattern).format(localDateTime)
}

private fun Instant.formatAbsenceDateTime(): String {
    val zoneId = ZoneId.systemDefault()
    return DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm 'Uhr'")
        .withZone(zoneId)
        .format(this)
}

@Composable
private fun ScannerFrame(
    backgroundColor: Color,
    bracketColor: Color = Color(0xFF0D47A1),
    showScanLine: Boolean = false,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(backgroundColor)
        )

        Canvas(modifier = Modifier.size(180.dp)) {
            val strokeWidth = 8.dp.toPx()
            val cornerLength = 40.dp.toPx()
            val cornerRadius = 20.dp.toPx()

            val path = Path().apply {
                moveTo(0f, cornerLength)
                lineTo(0f, cornerRadius)
                quadraticTo(0f, 0f, cornerRadius, 0f)
                lineTo(cornerLength, 0f)

                moveTo(size.width - cornerLength, 0f)
                lineTo(size.width - cornerRadius, 0f)
                quadraticTo(size.width, 0f, size.width, cornerRadius)
                lineTo(size.width, cornerLength)

                moveTo(size.width, size.height - cornerLength)
                lineTo(size.width, size.height - cornerRadius)
                quadraticTo(size.width, size.height, size.width - cornerRadius, size.height)
                lineTo(size.width - cornerLength, size.height)

                moveTo(cornerLength, size.height)
                lineTo(cornerRadius, size.height)
                quadraticTo(0f, size.height, 0f, size.height - cornerRadius)
                lineTo(0f, size.height - cornerLength)
            }

            drawPath(
                path = path,
                color = bracketColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        content()

        if (showScanLine) {
            val infiniteTransition = rememberInfiniteTransition(label = "ScanningTransition")
            val bias by infiniteTransition.animateFloat(
                initialValue = -0.7f,
                targetValue = 0.7f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "ScanLineBias"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(3.dp)
                    .background(colorScheme.secondary)
                    .align(BiasAlignment(0f, bias))
            )
        }
    }
}

@Composable
private fun AbsenceOverviewCard(absences: List<ReportedAbsence>) {
    val now = remember { Instant.now() }
    val sortedAbsences = remember(absences) {
        absences.sortedByDescending { it.fromAt }
    }

    BCICard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Gemeldete Abwesenheiten",
                style = typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            if (sortedAbsences.isEmpty()) {
                Text(
                    text = "Noch keine Abwesenheiten gemeldet.",
                    style = typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                )
            } else {
                sortedAbsences.forEach { absence ->
                    AbsenceOverviewEntry(absence = absence, now = now)
                }
            }
        }
    }
}

@Composable
private fun AbsenceOverviewEntry(
    absence: ReportedAbsence,
    now: Instant
) {
    val statusText = when {
        absence.status == "REJECTED" -> "Abgelehnt"
        absence.fromAt.isAfter(now) -> "Geplant"
        absence.untilAt.isAfter(now) -> "Aktiv"
        else -> "Abgelaufen"
    }
    val statusColor = when {
        absence.status == "REJECTED" -> colorScheme.onSurfaceVariant
        absence.fromAt.isAfter(now) -> colorScheme.primary
        absence.untilAt.isAfter(now) -> LocalCustomColors.current.warning
        else -> colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = statusText,
                style = typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = statusColor
            )

            Text(
                text = "Von ${absence.fromAt.formatAbsenceDateTime()}",
                style = typography.bodyMedium
            )
            Text(
                text = "Bis ${absence.untilAt.formatAbsenceDateTime()}",
                style = typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AbwesenheitSheet(
    onClose: () -> Unit,
    onReport: (Long, Long) -> Unit
) {
    val nowInstant = Instant.now()
    val zoneId = ZoneId.systemDefault()

    val today = LocalDate.now(zoneId)
        .atStartOfDay(ZoneId.of("UTC"))
        .toInstant()
        .toEpochMilli()

    val datePickerStateFrom = rememberDatePickerState(
        initialSelectedDateMillis = today
    )
    val datePickerStateTo = rememberDatePickerState(
        initialSelectedDateMillis = today
    )

    val currentLocalTime = LocalDateTime.now(zoneId)
    val timePickerStateFrom = rememberTimePickerState(
        initialHour = currentLocalTime.hour,
        initialMinute = currentLocalTime.minute
    )
    val timePickerStateTo = rememberTimePickerState(
        initialHour = (currentLocalTime.hour + 1).coerceAtMost(23),
        initialMinute = currentLocalTime.minute
    )

    var isFullDay by remember { mutableStateOf(false) }
    var showDatePickerFrom by remember { mutableStateOf(false) }
    var showDatePickerTo by remember { mutableStateOf(false) }
    var showTimePickerFrom by remember { mutableStateOf(false) }
    var showTimePickerTo by remember { mutableStateOf(false) }

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(zoneId)
    }

    val fromMillis by remember {
        derivedStateOf {
            calculateEpochMillis(
                datePickerStateFrom.selectedDateMillis,
                if (isFullDay) 0 else timePickerStateFrom.hour,
                if (isFullDay) 0 else timePickerStateFrom.minute
            )
        }
    }

    val toMillis by remember {
        derivedStateOf {
            calculateEpochMillis(
                datePickerStateTo.selectedDateMillis,
                if (isFullDay) 23 else timePickerStateTo.hour,
                if (isFullDay) 59 else timePickerStateTo.minute
            )
        }
    }

    val isFuture = remember(toMillis) {
        toMillis >= nowInstant.toEpochMilli()
    }

    val isRangeValid = remember(fromMillis, toMillis) {
        toMillis > fromMillis
    }

    val isValid = isFuture && isRangeValid

    val dateFromText = datePickerStateFrom.selectedDateMillis?.let {
        dateFormatter.format(Instant.ofEpochMilli(it))
    } ?: "Wählen..."

    val dateToText = datePickerStateTo.selectedDateMillis?.let {
        dateFormatter.format(Instant.ofEpochMilli(it))
    } ?: "Wählen..."


    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]

    val timeFromText =
        String.format(locale, "%02d:%02d", timePickerStateFrom.hour, timePickerStateFrom.minute)
    val timeToText =
        String.format(locale, "%02d:%02d", timePickerStateTo.hour, timePickerStateTo.minute)

    if (showDatePickerFrom) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerFrom = false },
            confirmButton = {
                TextButton(onClick = { showDatePickerFrom = false }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerStateFrom)
        }
    }

    if (showDatePickerTo) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerTo = false },
            confirmButton = {
                TextButton(onClick = { showDatePickerTo = false }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerStateTo)
        }
    }

    if (showTimePickerFrom) {
        TimePickerDialog(
            onDismissRequest = { showTimePickerFrom = false },
            onConfirm = { showTimePickerFrom = false }
        ) {
            TimePicker(state = timePickerStateFrom)
        }
    }

    if (showTimePickerTo) {
        TimePickerDialog(
            onDismissRequest = { showTimePickerTo = false },
            onConfirm = { showTimePickerTo = false }
        ) {
            TimePicker(state = timePickerStateTo)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .background(colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "Abwesenheit",
                    style = typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Abwesenheit melden",
                    style = typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Ganztägig",
                        style = typography.labelLarge,
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = isFullDay,
                        onCheckedChange = { isFullDay = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colorScheme.primary,
                            checkedTrackColor = colorScheme.primaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surfaceVariant.copy(
                        alpha = 0.3f
                    )
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Von", style = typography.bodyLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                onClick = { showDatePickerFrom = true },
                                color = colorScheme.surfaceVariant,
                                shape = CircleShape
                            ) {
                                Text(
                                    dateFromText,
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 6.dp
                                    ),
                                    style = typography.bodyMedium
                                )
                            }
                            if (!isFullDay) {
                                Surface(
                                    onClick = { showTimePickerFrom = true },
                                    color = colorScheme.surfaceVariant,
                                    shape = CircleShape
                                ) {
                                    Text(
                                        timeFromText,
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 6.dp
                                        ),
                                        style = typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Voraussichtlich bis", style = typography.bodyLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                onClick = { showDatePickerTo = true },
                                color = colorScheme.surfaceVariant,
                                shape = CircleShape
                            ) {
                                Text(
                                    dateToText,
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 6.dp
                                    ),
                                    style = typography.bodyMedium
                                )
                            }
                            if (!isFullDay) {
                                Surface(
                                    onClick = { showTimePickerTo = true },
                                    color = colorScheme.surfaceVariant,
                                    shape = CircleShape
                                ) {
                                    Text(
                                        timeToText,
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 6.dp
                                        ),
                                        style = typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!isValid) {
                val errorText = if (!isFuture) {
                    "Endzeitpunkt darf nicht in der Vergangenheit liegen"
                } else {
                    "Endzeitpunkt muss nach dem Startzeitpunkt liegen"
                }
                Text(
                    text = errorText,
                    color = colorScheme.error,
                    style = typography.labelSmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    onReport(fromMillis, toMillis)
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    "Abwesenheit melden",
                    style = typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

private fun calculateEpochMillis(dateMillis: Long?, hour: Int, minute: Int): Long {
    val date =
        dateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
            ?: LocalDate.now()
    return LocalDateTime.of(date, LocalTime.of(hour, minute))
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}

@Composable
private fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(intrinsicSize = IntrinsicSize.Min)
                .height(intrinsicSize = IntrinsicSize.Min)
                .background(shape = MaterialTheme.shapes.extraLarge, color = colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    text = "Zeit wählen",
                    style = typography.labelMedium
                )
                content()
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismissRequest
                    ) { Text("Abbrechen") }
                    TextButton(
                        onClick = onConfirm
                    ) { Text("OK") }
                }
            }
        }
    }
}

@Preview
@Composable
private fun StudentHomeScreenPreview() {
    AppTheme {
        StudentHomeScreen(
            uiState = HomeUiState(
                studentId = "123456",
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
                )
            ),
            onOpenSettings = {},
            onSelectDay = {},
            onRetryCheckIn = {},
            onOpenAbwesenheitSheet = {},
            onReportAbsence = { _, _ -> },
            onCloseSheet = {}
        )
    }
}

@Preview
@Composable
private fun StudentHomeScreenAbsenseSheetPreview() {
    AppTheme {
        StudentHomeScreen(
            uiState = HomeUiState(
                studentId = "123456",
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
                activeSheet = HomeSheet.Absence
            ),
            onOpenSettings = {},
            onSelectDay = {},
            onRetryCheckIn = {},
            onOpenAbwesenheitSheet = {},
            onReportAbsence = { _, _ -> },
            onCloseSheet = {}
        )
    }
}

@Preview
@Composable
private fun StudentHomeScreenTermineLeerPreview() {
    AppTheme {
        StudentHomeScreen(
            uiState = HomeUiState(
                studentId = "123456",
                studentName = "Max Mustermann",
            ),
            onOpenSettings = {},
            onSelectDay = {},
            onRetryCheckIn = {},
            onOpenAbwesenheitSheet = {},
            onReportAbsence = { _, _ -> },
            onCloseSheet = {}
        )
    }
}
