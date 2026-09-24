package com.example.clinexusapp

import com.example.clinexusapp.ui.screens.notifications.cleanNotificationMessage
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationMessageFormattingTest {

    @Test
    fun `serialized appointment midnight is removed but actual time remains`() {
        val message = "Your appointment on Thu Sep 24 2026 00:00:00 GMT+0000 (Coordinated Universal Time) at 3:45 PM has been confirmed."

        assertEquals(
            "Your appointment on Thu Sep 24 2026 at 3:45 PM has been confirmed.",
            cleanNotificationMessage(message)
        )
    }

    @Test
    fun `regular 24 hour time is still formatted`() {
        assertEquals(
            "Your appointment starts at 3:45 PM.",
            cleanNotificationMessage("Your appointment starts at 15:45:00.")
        )
    }
}
