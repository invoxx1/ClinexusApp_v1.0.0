package com.example.clinexusapp

import com.example.clinexusapp.model.AvailableSlotDTO
import com.example.clinexusapp.model.BookableServiceDTO
import com.example.clinexusapp.model.DentistDTO
import com.example.clinexusapp.util.Resource
import com.example.clinexusapp.viewmodel.BookingRules
import com.example.clinexusapp.viewmodel.BookingStep
import com.example.clinexusapp.viewmodel.BookingUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookingRulesTest {
    private val dentist = DentistDTO(1, "Dr. Test", "General", daysOfWeek = "Monday")
    private val service = BookableServiceDTO(2, "Cleaning", 500.0)
    private val slot = AvailableSlotDTO("10:00 AM", "10:00", "10:45")

    @Test fun eachStepRequiresItsOwnSelection() {
        assertTrue(!BookingRules.canContinue(BookingUiState()))
        assertTrue(BookingRules.canContinue(BookingUiState(step = BookingStep.DENTIST, selectedDentist = dentist)))
        assertTrue(BookingRules.canContinue(BookingUiState(step = BookingStep.SERVICE, selectedService = service)))
        assertTrue(BookingRules.canContinue(BookingUiState(step = BookingStep.DATE_TIME, selectedDate = "2026-09-10", selectedSlot = slot)))
        assertTrue(!BookingRules.canContinue(BookingUiState(step = BookingStep.REVIEW)))
        assertTrue(BookingRules.canContinue(BookingUiState(step = BookingStep.REVIEW, confirmationChecked = true)))
    }

    @Test fun unavailableSlotIsClearedButMatchingSlotIsPreserved() {
        val available = Resource.Success(listOf(slot))
        assertEquals(slot, BookingRules.keepSlotIfAvailable(slot, available))
        assertNull(BookingRules.keepSlotIfAvailable(slot, Resource.Success(emptyList())))
        assertNull(BookingRules.keepSlotIfAvailable(slot, Resource.Error("offline")))
    }
}