package com.example.clinexusapp

import com.example.clinexusapp.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class AppointmentDateTest {
    @Test
    fun `date-only appointment remains unchanged`() {
        assertEquals("2026-09-26", DateUtils.appointmentDateOnly("2026-09-26"))
    }

    @Test
    fun `legacy UTC serialization is restored to Manila clinic date`() {
        assertEquals(
            "2026-09-26",
            DateUtils.appointmentDateOnly("2026-09-25T16:00:00.000Z")
        )
    }
}
