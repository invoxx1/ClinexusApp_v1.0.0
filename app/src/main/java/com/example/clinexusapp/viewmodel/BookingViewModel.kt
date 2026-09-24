package com.example.clinexusapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clinexusapp.api.AppointmentRepository
import com.example.clinexusapp.model.AvailableSlotDTO
import com.example.clinexusapp.model.BookableServiceDTO
import com.example.clinexusapp.model.CreateAppointmentRequest
import com.example.clinexusapp.model.CreateAppointmentResponse
import com.example.clinexusapp.model.DentistDTO
import com.example.clinexusapp.model.DentistScheduleDTO
import com.example.clinexusapp.util.Resource
import com.example.clinexusapp.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import javax.inject.Inject
import java.io.Serializable

enum class BookingStep { DENTIST, SERVICE, DATE_TIME, REVIEW }

data class BookingUiState(
    val step: BookingStep = BookingStep.DENTIST,
    val dentists: Resource<List<DentistDTO>> = Resource.Loading,
    val services: Resource<List<BookableServiceDTO>> = Resource.Loading,
    val selectedDentist: DentistDTO? = null,
    val selectedServices: List<BookableServiceDTO> = emptyList(),
    val selectedDate: String? = null,
    val selectedSlot: AvailableSlotDTO? = null,
    val schedule: Resource<DentistScheduleDTO>? = null,
    val timeslots: Resource<List<AvailableSlotDTO>> = Resource.Success(emptyList()),
    val calendarAvailability: Map<String, Resource<List<AvailableSlotDTO>>> = emptyMap(),
    val submission: Resource<CreateAppointmentResponse>? = null,
    val confirmationChecked: Boolean = false,
    val isSubmitting: Boolean = false
)

data class AppointmentTicket(
    val reference: String,
    val status: String,
    val dentist: String,
    val service: String,
    val price: String,
    val date: String,
    val time: String,
    val clinic: String,
    val patient: String
) : Serializable

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val appointmentRepository: AppointmentRepository
) : ViewModel() {
    private var selectedDateRefreshJob: Job? = null
    private val serverRejectedSlots = mutableSetOf<String>()
    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState = _uiState.asStateFlow()

    init {
        fetchDentists()
        fetchServices()
    }

    private fun update(transform: (BookingUiState) -> BookingUiState) {
        _uiState.value = transform(_uiState.value)
    }

    private fun fetchDentists() = viewModelScope.launch {
        update { it.copy(dentists = Resource.Loading) }
        val result = appointmentRepository.getActiveDentists()
        update { it.copy(dentists = result) }
    }

    private fun fetchServices() = viewModelScope.launch {
        update { it.copy(services = Resource.Loading) }
        val result = appointmentRepository.getBookableServices()
        update {
            it.copy(services = if (result is Resource.Success) {
                Resource.Success(result.data.distinctBy(BookableServiceDTO::serviceId))
            } else result)
        }
    }

    fun retryDentists() { fetchDentists() }
    fun retryServices() { fetchServices() }

    fun selectDentist(dentist: DentistDTO) {
        update {
            it.copy(
                selectedDentist = dentist,
                selectedDate = null,
                selectedSlot = null,
                // Active-dentists already returns the authoritative active
                // dentist_schedules.day_of_week values from the backend.
                schedule = null,
                timeslots = Resource.Success(emptyList()),
                calendarAvailability = emptyMap(),
                confirmationChecked = false,
                step = BookingStep.DENTIST
            )
        }
    }

    fun toggleService(service: BookableServiceDTO) {
        update {
            val services = if (it.selectedServices.any { selected -> selected.serviceId == service.serviceId }) {
                it.selectedServices.filterNot { selected -> selected.serviceId == service.serviceId }
            } else {
                it.selectedServices + service
            }
            it.copy(
                selectedServices = services,
                selectedDate = null,
                selectedSlot = null,
                timeslots = Resource.Success(emptyList()),
                calendarAvailability = emptyMap(),
                confirmationChecked = false,
                step = BookingStep.SERVICE
            )
        }
    }

    fun selectDate(date: String) {
        if (_uiState.value.selectedDate == date && _uiState.value.timeslots is Resource.Loading) return
        selectedDateRefreshJob?.cancel()
        update { it.copy(selectedDate = date, selectedSlot = null, timeslots = Resource.Loading, confirmationChecked = false) }
        val dentist = _uiState.value.selectedDentist ?: return
        viewModelScope.launch {
            val slots = getBookableSlots(dentist.dentistId, date)
            update {
                it.copy(
                    timeslots = slots,
                    selectedSlot = BookingRules.keepSlotIfAvailable(it.selectedSlot, slots)
                )
            }
        }
    }

    fun loadCalendarAvailability(dates: List<String>) {
        val state = _uiState.value
        val dentist = state.selectedDentist ?: return
        if (state.totalEstimatedDurationMinutes <= 0) return
        val missing = dates.filter { it !in state.calendarAvailability }
        if (missing.isEmpty()) return
        update { it.copy(calendarAvailability = it.calendarAvailability + missing.associateWith { Resource.Loading }) }
        viewModelScope.launch {
            missing.forEach { date ->
                val result = getBookableSlots(dentist.dentistId, date, state.totalEstimatedDurationMinutes)
                update { current -> current.copy(calendarAvailability = current.calendarAvailability + (date to result)) }
            }
        }
    }

    fun retryCalendarAvailability(date: String) {
        update { it.copy(calendarAvailability = it.calendarAvailability - date) }
        loadCalendarAvailability(listOf(date))
    }

    fun refreshSelectedDate() {
        if (selectedDateRefreshJob?.isActive == true) return
        val state = _uiState.value
        val dentist = state.selectedDentist ?: return
        val date = state.selectedDate ?: return
        selectedDateRefreshJob = viewModelScope.launch {
            val slots = getBookableSlots(dentist.dentistId, date)
            update {
                if (it.selectedDate != date || it.selectedDentist?.dentistId != dentist.dentistId) return@update it
                // Preserve the patient's choice only while that slot is still available.
                it.copy(
                    timeslots = slots,
                    selectedSlot = BookingRules.keepSlotIfAvailable(it.selectedSlot, slots),
                    confirmationChecked = if (BookingRules.keepSlotIfAvailable(it.selectedSlot, slots) == null) false else it.confirmationChecked
                )
            }
        }
    }

    fun selectSlot(slot: AvailableSlotDTO) { update { it.copy(selectedSlot = slot, confirmationChecked = false) } }
    fun goTo(step: BookingStep) { update { it.copy(step = step) } }
    fun setConfirmationChecked(checked: Boolean) { update { it.copy(confirmationChecked = checked) } }

    fun submit() {
        val state = _uiState.value
        if (state.isSubmitting || !state.confirmationChecked) return
        val patientId = SessionManager.currentUser.value?.patientID
        val dentist = state.selectedDentist
        val services = state.selectedServices
        val date = state.selectedDate
        val slot = state.selectedSlot
        if (patientId == null || patientId == 0 || dentist == null || services.isEmpty() || date == null || slot?.startTime.isNullOrBlank() || slot.endTime.isNullOrBlank()) {
            update { it.copy(submission = Resource.Error("Please complete all appointment details.")) }
            return
        }
        if (!BookingRules.isFutureSlot(date, slot.startTime)) {
            update {
                it.copy(
                    step = BookingStep.DATE_TIME,
                    selectedSlot = null,
                    confirmationChecked = false,
                    submission = Resource.Error("That time has already passed. Please choose a later available time."),
                )
            }
            return
        }
        update { it.copy(isSubmitting = true, submission = Resource.Loading) }
        viewModelScope.launch {
            val result = appointmentRepository.createAppointment(
                CreateAppointmentRequest(
                    patientId = patientId,
                    dentistId = dentist.dentistId,
                    appointmentDate = date,
                    startTime = slot.startTime,
                    endTime = slot.endTime,
                    notes = "Mobile Booking",
                    selectedServices = services.map { it.serviceId }
                )
            )
            if (result is Resource.Error && result.message.isSlotConflictMessage()) {
                serverRejectedSlots += slotKey(dentist.dentistId, date, slot.startTime)
                val refreshedSlots = getBookableSlots(dentist.dentistId, date, state.totalEstimatedDurationMinutes)
                update {
                    it.copy(
                        step = BookingStep.DATE_TIME,
                        timeslots = refreshedSlots,
                        selectedSlot = null,
                        confirmationChecked = false,
                        submission = result,
                        isSubmitting = false
                    )
                }
            } else {
                update {
                    val remainingSlots = if (result is Resource.Success && it.timeslots is Resource.Success) {
                        Resource.Success(it.timeslots.data.filterNot { available ->
                            available.startTime?.take(5) == slot.startTime.take(5)
                        })
                    } else {
                        it.timeslots
                    }
                    it.copy(timeslots = remainingSlots, submission = result, isSubmitting = false)
                }
            }
        }
    }

    private suspend fun getBookableSlots(dentistId: Int, date: String, durationMinutes: Int = _uiState.value.totalEstimatedDurationMinutes): Resource<List<AvailableSlotDTO>> {
        if (durationMinutes <= 0) return Resource.Error("Please select at least one service.")
        val availableSlots = appointmentRepository.getAvailableTimeslots(dentistId, date, durationMinutes)
        if (availableSlots !is Resource.Success) return availableSlots
        val futureSlots = BookingRules.removePastSlots(availableSlots.data, date)
        val patientAppointments = appointmentRepository.getPatientAppointments()
        val patientFiltered = if (patientAppointments is Resource.Success) {
            BookingRules.removePatientConflicts(
                slots = futureSlots,
                appointments = patientAppointments.data,
                dentistId = dentistId,
                date = date,
            )
        } else {
            futureSlots
        }
        return Resource.Success(
            patientFiltered
                .filterNot { slotKey(dentistId, date, it.startTime) in serverRejectedSlots }
                .distinctBy { it.startTime?.take(5) }
        )
    }

    fun clearSubmission() { update { it.copy(submission = null) } }

    fun buildTicket(): AppointmentTicket? {
        val state = _uiState.value
        val patient = SessionManager.currentUser.value
        val response = state.submission
        if (response !is Resource.Success) return null
        return AppointmentTicket(
            reference = response.data.appointmentId?.toString() ?: "Pending",
            status = BookingRules.statusLabel(response.data.status),
            dentist = state.selectedDentist?.dentistName.orEmpty(),
            service = state.selectedServices.joinToString(", ") { it.serviceName ?: "Service" },
            price = "₱${String.format(java.util.Locale.US, "%,.0f", state.selectedServices.sumOf { it.price ?: 0.0 })}",
            date = state.selectedDate.orEmpty(),
            time = listOfNotNull(state.selectedSlot?.startTime, state.selectedSlot?.endTime).joinToString(" – "),
            clinic = "Clinexus Dental Clinic",
            patient = listOfNotNull(patient?.firstName, patient?.lastName).joinToString(" ").ifBlank { patient?.email.orEmpty() }
        )
    }
}

private fun slotKey(dentistId: Int, date: String, startTime: String?): String =
    "$dentistId|$date|${startTime?.take(5).orEmpty()}"

val BookingUiState.totalEstimatedDurationMinutes: Int
    get() = selectedServices.sumOf { it.durationMinutes ?: 0 }

private fun String?.isSlotConflictMessage(): Boolean {
    val value = this?.lowercase().orEmpty()
    return value.contains("already booked") || value.contains("no longer available") || value.contains("just booked")
}
