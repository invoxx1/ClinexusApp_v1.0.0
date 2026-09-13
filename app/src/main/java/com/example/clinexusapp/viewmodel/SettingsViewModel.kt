package com.example.clinexusapp.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

@HiltViewModel
class SettingsViewModel @Inject constructor(@ApplicationContext context: Context) : ViewModel() {
    private val preferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val _isDarkMode = MutableStateFlow(preferences.getBoolean("dark_mode", false))
    val isDarkMode = _isDarkMode.asStateFlow()
    private val _appointmentReminders = MutableStateFlow(preferences.getBoolean("appointment_reminders", true))
    val appointmentReminders = _appointmentReminders.asStateFlow()

    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        preferences.edit().putBoolean("dark_mode", enabled).apply()
    }

    fun toggleAppointmentReminders(enabled: Boolean) {
        _appointmentReminders.value = enabled
        preferences.edit().putBoolean("appointment_reminders", enabled).apply()
    }
}
