package com.example.clinexusapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clinexusapp.api.AppointmentRepository
import com.example.clinexusapp.model.AppointmentDTO
import com.example.clinexusapp.model.AvailableSlotDTO
import com.example.clinexusapp.model.CancelAppointmentRequest
import com.example.clinexusapp.model.RescheduleAppointmentRequest
import com.example.clinexusapp.util.Resource
import com.example.clinexusapp.util.AppointmentReminderScheduler
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class AppointmentTab { PENDING, CONFIRMED, COMPLETED, CANCELLED }

data class HistoryUiState(
    val appointments: Resource<List<AppointmentDTO>> = Resource.Loading,
    val selectedTab: AppointmentTab = AppointmentTab.PENDING,
    val operation: Resource<String>? = null,
    val completedRequest: String? = null,
    val cancellationSubmitting: Boolean = false,
    val rescheduleSubmitting: Boolean = false,
    val rescheduleSlots: Resource<List<AvailableSlotDTO>> = Resource.Success(emptyList())
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState = _uiState.asStateFlow()
    val historyState = _uiState.map { it.appointments }

    fun fetchHistory(clearOperation: Boolean = true, showLoading: Boolean = true) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                appointments = if (showLoading) Resource.Loading else _uiState.value.appointments,
                operation = if (clearOperation) null else _uiState.value.operation
            )
            val result = appointmentRepository.getPatientAppointments()
            _uiState.value = _uiState.value.copy(appointments = result)
            if (result is Resource.Success) AppointmentReminderScheduler.sync(context, result.data)
        }
    }

    fun selectTab(tab: AppointmentTab) { _uiState.value = _uiState.value.copy(selectedTab = tab) }

    fun cancelAppointment(id: Int, reason: String) {
        if (_uiState.value.cancellationSubmitting) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cancellationSubmitting = true, operation = Resource.Loading)
            val result = appointmentRepository.cancelAppointment(id, CancelAppointmentRequest(reason))
            if (result is Resource.Success) {
                _uiState.value = _uiState.value.copy(
                    cancellationSubmitting = false,
                    operation = Resource.Success("Cancellation request submitted. Waiting for clinic approval."),
                    completedRequest = "Cancellation request submitted. Waiting for clinic approval."
                )
                fetchHistory(clearOperation = false)
            } else {
                _uiState.value = _uiState.value.copy(cancellationSubmitting = false, operation = Resource.Error((result as Resource.Error).message))
            }
        }
    }

    fun loadRescheduleSlots(appointment: AppointmentDTO, date: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(rescheduleSlots = Resource.Loading)
            _uiState.value = _uiState.value.copy(rescheduleSlots = appointmentRepository.getAvailableTimeslots(appointment.dentistId, date))
        }
    }

    fun requestReschedule(appointment: AppointmentDTO, date: String, slot: AvailableSlotDTO, note: String) {
        if (_uiState.value.rescheduleSubmitting) return
        if (note.isBlank() && !appointment.needsPatientScheduleChoice) {
            _uiState.value = _uiState.value.copy(operation = Resource.Error("Please provide a reason for your reschedule request."))
            return
        }
        if (isUnchangedReschedule(appointment, date, slot)) {
            _uiState.value = _uiState.value.copy(operation = Resource.Error("Choose a different date or time."))
            return
        }
        val start = slot.startTime ?: return
        val end = slot.endTime ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(rescheduleSubmitting = true, operation = Resource.Loading)
            val result = appointmentRepository.rescheduleAppointment(
                appointment.appointmentId,
                RescheduleAppointmentRequest(
                    date,
                    start,
                    end,
                    note.trim().ifBlank { "New schedule selected after the clinic requested rescheduling." },
                    appointment.dentistId
                )
            )
            if (result is Resource.Success) {
                val successMessage = if (appointment.needsPatientScheduleChoice) {
                    "Your new schedule was submitted. Waiting for clinic approval."
                } else {
                    "Reschedule request submitted. Waiting for clinic approval."
                }
                _uiState.value = _uiState.value.copy(
                    rescheduleSubmitting = false,
                    operation = Resource.Success(successMessage),
                    completedRequest = successMessage
                )
                fetchHistory(clearOperation = false)
            } else {
                val serverMessage = (result as Resource.Error).message
                val message = if (
                    appointment.needsPatientScheduleChoice &&
                    serverMessage?.contains("can no longer be rescheduled", ignoreCase = true) == true
                ) {
                    "We couldn't submit your new schedule yet. Please try again in a moment."
                } else {
                    serverMessage
                }
                _uiState.value = _uiState.value.copy(
                    rescheduleSubmitting = false,
                    operation = Resource.Error(message)
                )
            }
        }
    }

    fun clearOperation() { _uiState.value = _uiState.value.copy(operation = null) }

    fun clearCompletedRequest() { _uiState.value = _uiState.value.copy(completedRequest = null) }

    fun filteredAppointments(tab: AppointmentTab, source: List<AppointmentDTO>): List<AppointmentDTO> {
        return filterAppointmentsForTab(tab, source)
    }

    private fun appointmentDateTime(appointment: AppointmentDTO): Date? = runCatching {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).parse("${appointment.appointmentDate.substringBefore('T')} ${appointment.startTime}")
    }.getOrNull()
}

fun filterAppointmentsForTab(tab: AppointmentTab, source: List<AppointmentDTO>): List<AppointmentDTO> =
    source.filter { mapAppointmentStatus(it.appointmentStatus).toTab() == tab }
        .sortedByDescending {
            runCatching {
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).parse("${it.appointmentDate.substringBefore('T')} ${it.startTime}")
            }.getOrNull()
        }

fun isUnchangedReschedule(appointment: AppointmentDTO, date: String, slot: AvailableSlotDTO): Boolean =
    date == appointment.appointmentDate.substringBefore("T") && slot.startTime == appointment.startTime

fun AppointmentStatus.toTab(): AppointmentTab = when (this) {
    AppointmentStatus.PENDING, AppointmentStatus.RESCHEDULE_REQUESTED, AppointmentStatus.CANCELLATION_REQUESTED -> AppointmentTab.PENDING
    AppointmentStatus.CONFIRMED -> AppointmentTab.CONFIRMED
    AppointmentStatus.COMPLETED -> AppointmentTab.COMPLETED
    AppointmentStatus.CANCELLED -> AppointmentTab.CANCELLED
    AppointmentStatus.UNKNOWN -> AppointmentTab.PENDING
}
