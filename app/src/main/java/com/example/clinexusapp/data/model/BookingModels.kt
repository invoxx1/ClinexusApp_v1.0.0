package com.example.clinexusapp.data.model

import androidx.compose.ui.graphics.vector.ImageVector

data class DentalService(
    val id: String,
    val name: String,
    val durationMinutes: Int,
    val priceEstimate: String,
    val icon: ImageVector? = null, // Using null for now or specific icons if available
    val emoji: String = "🦷"
)

data class TimeSlot(
    val time: String, // e.g., "09:00 AM"
    val hour: Int,    // 9, 10, 11, etc.
    val minute: Int,  // 0, 15, 30, 45
    val dentistsOccupied: Int = 0,
    val maxCapacity: Int = 2
) {
    val isFullyBooked: Boolean get() = dentistsOccupied >= maxCapacity
    val hasLimitedAvailability: Boolean get() = dentistsOccupied == maxCapacity - 1
}

data class Booking(
    val id: String,
    val serviceId: String,
    val startTimeString: String, // "09:00 AM"
    val durationMinutes: Int,
    val dentistId: Int
)

