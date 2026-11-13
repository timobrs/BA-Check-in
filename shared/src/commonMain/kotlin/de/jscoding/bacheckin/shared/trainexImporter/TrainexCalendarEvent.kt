package de.jscoding.bacheckin.shared.trainexImporter

import kotlinx.datetime.LocalDateTime

data class TrainexCalendarEvent(
    val title: String,
    val location: String,
    val course: String,
    val lecturer: String,
    val start: LocalDateTime?,
    val end: LocalDateTime?
)
