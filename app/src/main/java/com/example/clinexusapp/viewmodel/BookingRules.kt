package com.example.clinexusapp.viewmodel

import com.example.clinexusapp.model.AvailableSlotDTO
import com.example.clinexusapp.model.AppointmentDTO
import com.example.clinexusapp.util.Resource

object BookingRules {
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
