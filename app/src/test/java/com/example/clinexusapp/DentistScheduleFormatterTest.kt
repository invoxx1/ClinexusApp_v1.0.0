package com.example.clinexusapp

import com.example.clinexusapp.viewmodel.DentistScheduleFormatter
import com.example.clinexusapp.viewmodel.BookingRules
import com.example.clinexusapp.viewmodel.AppointmentStatus
import com.example.clinexusapp.viewmodel.mapAppointmentStatus
import com.example.clinexusapp.viewmodel.AppointmentTab
import com.example.clinexusapp.viewmodel.filterAppointmentsForTab
import com.example.clinexusapp.viewmodel.isUnchangedReschedule
import com.example.clinexusapp.model.AppointmentDTO
import com.example.clinexusapp.model.AvailableSlotDTO
import org.junit.Assert.assertEquals
import org.junit.Test

class DentistScheduleFormatterTest {
    @Test fun formatsCompactSchedules() {
        assertEquals("Available Monday", DentistScheduleFormatter.format("Monday"))
        assertEquals("Available Mon–Tue", DentistScheduleFormatter.format("Tuesday, Monday"))
        assertEquals("Available Mon, Wed", DentistScheduleFormatter.format("Monday, Wednesday"))
        assertEquals("Available Mon–Wed", DentistScheduleFormatter.format("Wednesday, Monday, Tuesday"))
        assertEquals("Available Mon–Tue, Thu", DentistScheduleFormatter.format("Monday, Tuesday, Thursday"))
        assertEquals("Available Mon, Wed, Fri", DentistScheduleFormatter.format("Friday, Monday, Wednesday"))
        assertEquals("Available Mon–Tue, Thu–Fri", DentistScheduleFormatter.format("Friday, Tuesday, Thursday, Monday"))
        assertEquals("Available Mon–Sat", DentistScheduleFormatter.format("Monday, Tuesday, Wednesday, Thursday, Friday, Saturday"))
        assertEquals("Available daily", DentistScheduleFormatter.format("Sunday, Monday, Tuesday, Wednesday, Thursday, Friday, Saturday"))
    }

    @Test fun handlesEmptyInvalidAndDuplicateDays() {
        assertEquals("No schedule available", DentistScheduleFormatter.format(null))
        assertEquals("No schedule available", DentistScheduleFormatter.format("Notaday"))
        assertEquals("Available Mon–Tue", DentistScheduleFormatter.format("tue, MONDAY, Monday"))
    }
    
        @Test fun mapsBackendStatusesWithoutCallingRequestsConfirmed() {
            assertEquals("Awaiting clinic approval", BookingRules.statusLabel(null))
            assertEquals("Awaiting clinic approval", BookingRules.statusLabel("requested"))
            assertEquals("Awaiting clinic approval", BookingRules.statusLabel("pending"))
            assertEquals("Confirmed", BookingRules.statusLabel("confirmed"))
            assertEquals("Confirmed", BookingRules.statusLabel("scheduled"))
        }

        @Test fun mapsAppointmentStatusesAndUnknownValuesSafely() {
            assertEquals(AppointmentStatus.PENDING, mapAppointmentStatus("REQUESTED"))
            assertEquals(AppointmentStatus.CONFIRMED, mapAppointmentStatus("UPCOMING"))
            assertEquals(AppointmentStatus.COMPLETED, mapAppointmentStatus("DONE"))
            assertEquals(AppointmentStatus.CANCELLED, mapAppointmentStatus("CANCELED"))
            assertEquals(AppointmentStatus.RESCHEDULE_REQUESTED, mapAppointmentStatus("RESCHEDULE_REQUESTED"))
            assertEquals(AppointmentStatus.UNKNOWN, mapAppointmentStatus("something_new"))
        }

        @Test fun filtersAndSortsAppointmentTabs() {
            fun appointment(status: String, date: String) = AppointmentDTO(1, 1, 1, date, "09:00", "09:30", "Cleaning", status, null, "A", "Dentist")
            val items = listOf(appointment("CONFIRMED", "2026-09-11"), appointment("CONFIRMED", "2026-09-15"), appointment("COMPLETED", "2026-09-01"))
            assertEquals(2, filterAppointmentsForTab(AppointmentTab.CONFIRMED, items).size)
            assertEquals("2026-09-15", filterAppointmentsForTab(AppointmentTab.CONFIRMED, items).first().appointmentDate)
            assertEquals(1, filterAppointmentsForTab(AppointmentTab.COMPLETED, items).size)
        }

        @Test fun rejectsAnUnchangedReschedule() {
            val appointment = AppointmentDTO(1, 1, 1, "2026-09-11", "09:00", "09:30", "Cleaning", "CONFIRMED", null, "A", "Dentist")
            assertEquals(true, isUnchangedReschedule(appointment, "2026-09-11", AvailableSlotDTO("9:00", "09:00", "09:30")))
        }
}