package de.jscoding.bacheckin.shared.trainexImporter

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.char

object IcsReader {

    fun read(icalFile: String): List<TrainexCalendarEvent>{
        val events = mutableListOf<TrainexCalendarEvent>()

        for(event in icalFile.split("BEGIN:VEVENT").drop(1)){


            println("--EVENT--")
            println(event)

            val title = event.substringAfter("SUMMARY:").substringBefore(" - ")

            val location = event.substringAfter("LOCATION:").substringBefore(" - ")

            val course = event.substringAfter("DESCRIPTION:").substringAfter("(").substringBefore(")")

            var lecturer = event.substringAfter("DESCRIPTION:").substringAfter("$course)").substringBefore("ab")
            lecturer = lecturer.substringAfter("/").trim()

            val start = event.substringAfter("DTSTART:").substringBefore("\n")
            val startDate = parseDateTime(start)

            val end = event.substringAfter("DTEND:").substringBefore("\n")
            val endDate = parseDateTime(end)

            val event = TrainexCalendarEvent(
                title = title,
                location = location,
                course = course,
                lecturer = lecturer,
                start = startDate,
                end = endDate
            )

            events.add(event)
        }
        return events
    }

    val dateFormat = LocalDateTime.Format {
        year(); monthNumber(); day()
        char('T')
        hour(); minute(); second()
    }

    fun parseDateTime(rawDateString: String): LocalDateTime? {
        val cleanDateString = rawDateString.take(15)
        return try {
            LocalDateTime.parse(cleanDateString, dateFormat)
        } catch (_: Exception) {
            null
        }
    }

}
