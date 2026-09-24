package com.example.clinexusapp.viewmodel

import com.example.clinexusapp.model.AvailableSlotDTO
import com.example.clinexusapp.model.AppointmentDTO
import com.example.clinexusapp.util.Resource
import java.time.LocalDate
import java.time.LocalTime

object BookingRules {
    val clinicZone: java.time.ZoneId = java.time.ZoneId.of("Asia/Manila")
    fun isWithinBookingWindow(date: String, today: LocalDate = LocalDate.now(clinicZone)): Boolean =
        runCatching { LocalDate.parse(date).let { it.isAfter(today) && !it.isAfter(today.plusDays(30)) } }.getOrDefault(false)

    fun statusLabel(rawStatus: String?): String {
        val value = rawStatus?.trim()?.lowercase().orEmpty()
        return when {
            value.isBlank() || value.contains("pending") || value.contains("request") || value.contains("await") || value.contains("approval") -> "Awaiting clinic approval"
            value.contains("cancel") -> "Cancelled"
            value.contains("confirm") || value.contains("scheduled") || value.contains("approved") -> "Confirmed"
            value.contains("reject") || value.contains("declin") || value.contains("den") -> "Rejected"
            else -> "Awaiting clinic approval"
        }
    }

    fun canContinue(state: BookingUiState): Boolean = when (state.step) {
        BookingStep.DENTIST -> state.selectedDentist != null
        BookingStep.SERVICE -> state.selectedServices.isNotEmpty()
        BookingStep.DATE_TIME -> (state.selectedDate != null) && (state.selectedSlot != null)
        BookingStep.REVIEW -> state.confirmationChecked && !state.isSubmitting
    }

    fun keepSlotIfAvailable(slot: AvailableSlotDTO?, slots: Resource<List<AvailableSlotDTO>>): AvailableSlotDTO? =
        slot?.takeIf { selected -> slots is Resource.Success && slots.data.any { it.startTime == selected.startTime } }

    fun removePastSlots(
        slots: List<AvailableSlotDTO>,
        date: String,
        today: String = LocalDate.now().toString(),
        currentMinutes: Int = LocalTime.now().hour * 60 + LocalTime.now().minute,
    ): List<AvailableSlotDTO> = when {
        date < today -> emptyList()
        date > today -> slots
        else -> slots.filter { (it.startTime.toMinutes() ?: -1) > currentMinutes }
    }

    fun isFutureSlot(
        date: String,
        startTime: String?,
        today: String = LocalDate.now().toString(),
        currentMinutes: Int = LocalTime.now().hour * 60 + LocalTime.now().minute,
    ): Boolean = date > today || (date == today && (startTime.toMinutes() ?: -1) > currentMinutes)

    fun removePatientConflicts(
        slots: List<AvailableSlotDTO>,
        appointments: List<AppointmentDTO>,
        dentistId: Int,
        date: String,
    ): List<AvailableSlotDTO> {
        val blocked = appointments.filter { appointment ->
            appointment.dentistId == dentistId &&
                appointment.appointmentDate.substringBefore('T') == date &&
                appointment.appointmentStatus.trim().lowercase() !in setOf(
                    "cancelled", "canceled", "completed", "done", "rejected", "declined", "denied", "no_show"
                )
        }
        return slots.filterNot { slot ->
            val slotStart = slot.startTime.toMinutes() ?: return@filterNot false
            val slotEnd = slot.endTime.toMinutes() ?: return@filterNot false
            blocked.any { appointment ->
                val bookedStart = appointment.startTime.toMinutes() ?: return@any false
                val bookedEnd = appointment.endTime.toMinutes() ?: return@any false
                slotStart < bookedEnd && slotEnd > bookedStart
            }
        }
    }

    private fun String?.toMinutes(): Int? {
        val parts = this?.take(5)?.split(":") ?: return null
        if (parts.size != 2) return null
        return (parts[0].toIntOrNull() ?: return null) * 60 + (parts[1].toIntOrNull() ?: return null)
    }
}

enum class AppointmentStatus {
    PENDING, CONFIRMED, IN_PROGRESS, COMPLETED, NO_SHOW, CANCELLED, RESCHEDULE_REQUESTED, CANCELLATION_REQUESTED, UNKNOWN
}

fun mapAppointmentStatus(raw: String?): AppointmentStatus {
    val value = raw?.trim()?.lowercase().orEmpty()
    return when {
        value == "reschedule_requested" || value == "needs_reschedule" || value.contains("reschedule") -> AppointmentStatus.RESCHEDULE_REQUESTED
        value == "needs_cancellation" -> AppointmentStatus.CANCELLATION_REQUESTED
        value == "cancellation_approved" || value == "cancel_approved" -> AppointmentStatus.CANCELLED
        value == "pending" || value == "requested" || value == "request" || value.contains("await") -> AppointmentStatus.PENDING
        value == "approved" || value == "confirmed" || value == "scheduled" || value == "upcoming" -> AppointmentStatus.CONFIRMED
        value == "in_progress" -> AppointmentStatus.IN_PROGRESS
        value == "no_show" -> AppointmentStatus.NO_SHOW
        value == "completed" || value == "done" -> AppointmentStatus.COMPLETED
        value == "cancelled" || value == "canceled" || value == "rejected" || value == "declined" || value == "denied" -> AppointmentStatus.CANCELLED
        else -> AppointmentStatus.UNKNOWN
    }
}

fun AppointmentStatus.patientLabel(): String = when (this) {
    AppointmentStatus.PENDING -> "Pending"
    AppointmentStatus.CONFIRMED -> "Confirmed"
    AppointmentStatus.IN_PROGRESS -> "In progress"
    AppointmentStatus.COMPLETED -> "Completed"
    AppointmentStatus.NO_SHOW -> "No show"
    AppointmentStatus.CANCELLED -> "Cancelled"
    AppointmentStatus.RESCHEDULE_REQUESTED -> "Reschedule requested"
    AppointmentStatus.CANCELLATION_REQUESTED -> "Cancellation requested"
    AppointmentStatus.UNKNOWN -> "Status unavailable"
}
