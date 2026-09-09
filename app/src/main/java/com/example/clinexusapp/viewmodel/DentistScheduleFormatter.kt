package com.example.clinexusapp.viewmodel

import java.util.Locale

object DentistScheduleFormatter {
    private val orderedDays = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
    private val shortDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    fun format(days: String?): String {
        val indexes = days.orEmpty()
            .split(",", "-", "–", "/")
            .map { normalize(it) }
            .mapNotNull { value -> orderedDays.indexOf(value).takeIf { it >= 0 } }
            .distinct()
            .sorted()

        if (indexes.isEmpty()) return "No schedule available"
        if (indexes.size == orderedDays.size) return "Available daily"

        val groups = mutableListOf<IntRange>()
        var start = indexes.first()
        var previous = start
        indexes.drop(1).forEach { current ->
            if (current == (previous + 1)) {
                previous = current
            } else {
                groups += start..previous
                start = current
                previous = current
            }
        }
        groups += start..previous

        return "Available " + groups.joinToString(", ") { range ->
            when (range.count()) {
                1 -> if (indexes.size == 1) orderedDays[range.first].replaceFirstChar { it.uppercase() } else shortDays[range.first]
                2 -> "${shortDays[range.first]}–${shortDays[range.last]}"
                else -> "${shortDays[range.first]}–${shortDays[range.last]}"
            }
        }
    }

    private fun normalize(value: String): String {
        val normalized = value.trim().lowercase(Locale.US)
        return orderedDays.firstOrNull { it == normalized || it.take(3) == normalized.take(3) }.orEmpty()
    }
}