package com.example.clinexusapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clinexusapp.api.AppointmentRepository
import com.example.clinexusapp.model.AppointmentDTO
import com.example.clinexusapp.model.AvailableSlotDTO
import com.example.clinexusapp.model.CancelAppointmentRequest
import com.example.clinexusapp.model.RescheduleAppointmentRequest
import com.example.clinexusapp.util.Resource
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
    val cancellationSubmitting: Boolean = false,
    val rescheduleSubmitting: Boolean = false,
    val rescheduleSlots: Resource<List<AvailableSlotDTO>> = Resource.Success(emptyList())
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val appointmentRepository: AppointmentRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState = _uiState.asStateFlow()
    val historyState = _uiState.map { it.appointments }

    init { fetchHistory() }

    fun fetchHistory(clearOperation: Boolean = true) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(appointments = Resource.Loading, operation = if (clearOperation) null else _uiState.value.operation)
            _uiState.value = _uiState.value.copy(appointments = appointmentRepository.getPatientAppointments())
        }
    }

    fun selectTab(tab: AppointmentTab) { _uiState.value = _uiState.value.copy(selectedTab = tab) }

    fun cancelAppointment(id: Int, reason: String) {
        if (_uiState.value.cancellationSubmitting) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cancellationSubmitting = true, operation = Resource.Loading)
            val result = appointmentRepository.cancelAppointment(id, CancelAppointmentRequest(reason))
            if (result is Resource.Success) {
                _uiState.value = _uiState.value.copy(cancellationSubmitting = false, operation = Resource.Success("Cancellation request sent."))
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
                RescheduleAppointmentRequest(date, start, end, note, appointment.dentistId)
            )
            if (result is Resource.Success) {
                _uiState.value = _uiState.value.copy(rescheduleSubmitting = false, operation = Resource.Success("Reschedule request sent."))
                fetchHistory(clearOperation = false)
            } else {
                _uiState.value = _uiState.value.copy(rescheduleSubmitting = false, operation = Resource.Error((result as Resource.Error).message))
            }
        }
    }

    fun clearOperation() { _uiState.value = _uiState.value.copy(operation = null) }

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
    AppointmentStatus.PENDING, AppointmentStatus.RESCHEDULE_REQUESTED -> AppointmentTab.PENDING
    AppointmentStatus.CONFIRMED -> AppointmentTab.CONFIRMED
    AppointmentStatus.COMPLETED -> AppointmentTab.COMPLETED
    AppointmentStatus.CANCELLED -> AppointmentTab.CANCELLED
    AppointmentStatus.UNKNOWN -> AppointmentTab.PENDING
}