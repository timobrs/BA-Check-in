package de.jscoding.bacheckin.feature.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.jscoding.bacheckin.core.ui.components.BCICard
import de.jscoding.bacheckin.core.ui.theme.courseBlue
import de.jscoding.bacheckin.core.ui.theme.courseTeal
import de.jscoding.bacheckin.core.ui.theme.onSurfaceDark
import de.jscoding.bacheckin.feature.home.Appointment
import de.jscoding.bacheckin.feature.home.Day
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeAppointmentCard(
    modifier: Modifier = Modifier,
    appointments: List<Appointment>,
    selectedDay: Day,
    onSelectDay: (Day) -> Unit,
) {
    val zoneId = ZoneId.systemDefault()
    val selectedDate = selectedDay.toDate(zoneId)
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(zoneId)

    BCICard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Meine Termine",
                style = typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DayTab(
                    text = "Heute",
                    selected = selectedDay == Day.Today,
                    onClick = { onSelectDay(Day.Today) }
                )
                DayTab(
                    text = "Morgen",
                    selected = selectedDay == Day.Tomorrow,
                    onClick = { onSelectDay(Day.Tomorrow) }
                )
            }

            val appointmentsToDisplay = appointments
                .filter { it.startsOn(selectedDate, zoneId) }
                .sortedBy { it.startAt }

            appointmentsToDisplay.forEachIndexed { index, appointment ->
                val containerColor = if (index % 2 == 0) courseTeal else courseBlue
                val contentColor = onSurfaceDark

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = containerColor,
                        contentColor = contentColor
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            appointment.title,
                            style = typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = appointment.room,
                                style = typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                fontStyle = FontStyle.Italic
                            )
                        }
                        Text(
                            appointment.timeText(timeFormatter),
                            style = typography.bodyMedium.copy(fontStyle = FontStyle.Italic)
                        )
                    }
                }
            }

            if (appointmentsToDisplay.isEmpty()) {
                Text(
                    text = "Keine Termine für diesen Tag.",
                    style = typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun Day.toDate(zoneId: ZoneId): LocalDate =
    when (this) {
        Day.Today -> LocalDate.now(zoneId)
        Day.Tomorrow -> LocalDate.now(zoneId).plusDays(1)
    }

private fun Appointment.startsOn(date: LocalDate, zoneId: ZoneId): Boolean =
    startAt.atZone(zoneId).toLocalDate() == date

private fun Appointment.timeText(formatter: DateTimeFormatter): String =
    "${formatter.format(startAt)}-${formatter.format(endAt)} Uhr"


@Composable
private fun DayTab(text: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(40.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) colorScheme.primary else colorScheme.surfaceVariant,
            contentColor = if (selected) colorScheme.onPrimary else colorScheme.onSurfaceVariant
        ),
        shape = CircleShape
    ) {
        Text(
            text = text,
            style = typography.labelLarge
        )
    }
}
