package com.example.clinexusapp.util

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppNavigationRequests {
    const val ACTION_OPEN_APPOINTMENT = "com.example.clinexusapp.OPEN_APPOINTMENT"
    const val EXTRA_APPOINTMENT_ID = "appointment_id"

    private val _appointmentId = MutableStateFlow<Int?>(null)
    val appointmentId = _appointmentId.asStateFlow()

    fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_OPEN_APPOINTMENT) {
            intent.getIntExtra(EXTRA_APPOINTMENT_ID, -1).takeIf { it > 0 }?.let { _appointmentId.value = it }
        }
    }

    fun consumeAppointment() { _appointmentId.value = null }
}
