package com.example.clinexusapp

import com.example.clinexusapp.model.AppointmentDTO
import com.example.clinexusapp.viewmodel.AppointmentStatus
import com.example.clinexusapp.viewmodel.mapAppointmentStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardLogicTest {

    @Test
    fun `mapAppointmentStatus maps Pending correctly`() {
        assertEquals(AppointmentStatus.PENDING, mapAppointmentStatus("PENDING"))
        assertEquals(AppointmentStatus.PENDING, mapAppointmentStatus("REQUESTED"))
        assertEquals(AppointmentStatus.PENDING, mapAppointmentStatus("pending"))
    }

    @Test
    fun `mapAppointmentStatus maps Confirmed correctly`() {
        assertEquals(AppointmentStatus.CONFIRMED, mapAppointmentStatus("CONFIRMED"))
        assertEquals(AppointmentStatus.CONFIRMED, mapAppointmentStatus("APPROVED"))
        assertEquals(AppointmentStatus.CONFIRMED, mapAppointmentStatus("scheduled"))
        assertEquals(AppointmentStatus.CONFIRMED, mapAppointmentStatus("upcoming"))
    }

    @Test
    fun `mapAppointmentStatus maps Reschedule correctly`() {
        assertEquals(AppointmentStatus.RESCHEDULE_REQUESTED, mapAppointmentStatus("RESCHEDULE_REQUESTED"))
        assertEquals(AppointmentStatus.RESCHEDULE_REQUESTED, mapAppointmentStatus("needs_reschedule"))
    }

    @Test
    fun `mapAppointmentStatus maps Cancelled correctly`() {
        assertEquals(AppointmentStatus.CANCELLED, mapAppointmentStatus("CANCELLED"))
        assertEquals(AppointmentStatus.CANCELLED, mapAppointmentStatus("CANCELED"))
        assertEquals(AppointmentStatus.CANCELLED, mapAppointmentStatus("CANCELLATION_APPROVED"))
        assertEquals(AppointmentStatus.CANCELLED, mapAppointmentStatus("rejected"))
    }

    @Test
    fun `sorting logic priority is Confirmed over Pending`() {
        val appts = listOf(
            createAppt(1, "pending", "2026-09-10", "10:00"),
            createAppt(2, "confirmed", "2026-09-10", "11:00")
        )
        
        val sorted = appts.sortedWith(compareBy<AppointmentDTO> {
            val status = it.appointmentStatus.lowercase()
            when {
                status.contains("confirm") || status.contains("approved") || status.contains("scheduled") -> 0
                status.contains("pending") || status.contains("request") -> 1
                status.contains("reschedule") -> 2
                else -> 3
            }
        }.thenBy { it.startTime })
        
        assertEquals(2, sorted[0].appointmentId)
    }

    private fun createAppt(id: Int, status: String, date: String, time: String) = AppointmentDTO(
        appointmentId = id,
        patientId = 1,
        dentistId = 1,
        appointmentDate = date,
        startTime = time,
        endTime = "11:00",
        appointmentType = "Consultation",
        appointmentStatus = status,
        notes = null,
        dentistFirstName = "Doc",
        dentistLastName = "Test",
        dentistSpecialty = "General",
        dentistProfileImage = null,
        serviceName = "Consultation",
        price = 100.0,
        clinicName = "Clinic",
        submittedAt = "2026-09-09",
        cancelledBy = null,
        cancelledAt = null,
        cancellationReason = null,
        rescheduleNote = null
    )
}
