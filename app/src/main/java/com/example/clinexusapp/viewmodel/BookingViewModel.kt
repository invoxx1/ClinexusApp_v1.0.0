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
import javax.inject.Inject
import java.io.Serializable

enum class BookingStep { DENTIST, SERVICE, DATE_TIME, REVIEW }

data class BookingUiState(
    val step: BookingStep = BookingStep.DENTIST,
    val dentists: Resource<List<DentistDTO>> = Resource.Loading,
    val services: Resource<List<BookableServiceDTO>> = Resource.Loading,
    val selectedDentist: DentistDTO? = null,
    val selectedService: BookableServiceDTO? = null,
    val selectedDate: String? = null,
    val selectedSlot: AvailableSlotDTO? = null,
    val schedule: Resource<DentistScheduleDTO>? = null,
    val timeslots: Resource<List<AvailableSlotDTO>> = Resource.Success(emptyList()),
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
                schedule = null,
                timeslots = Resource.Success(emptyList()),
                confirmationChecked = false,
                step = BookingStep.DENTIST
            )
        }
    }

    fun selectService(service: BookableServiceDTO) {
        update {
            it.copy(
                selectedService = service,
                selectedDate = null,
                selectedSlot = null,
                timeslots = Resource.Success(emptyList()),
                confirmationChecked = false,
                step = BookingStep.SERVICE
            )
        }
    }

    fun selectDate(date: String) {
        if (_uiState.value.selectedDate == date && _uiState.value.timeslots is Resource.Loading) return
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

    fun refreshSelectedDate() {
        val state = _uiState.value
        val dentist = state.selectedDentist ?: return
        val date = state.selectedDate ?: return
        viewModelScope.launch {
            val slots = getBookableSlots(dentist.dentistId, date)
            update {
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
        val service = state.selectedService
        val date = state.selectedDate
        val slot = state.selectedSlot
        if (patientId == null || patientId == 0 || dentist == null || service == null || date == null || slot?.startTime.isNullOrBlank() || slot.endTime.isNullOrBlank()) {
            update { it.copy(submission = Resource.Error("Please complete all appointment details.")) }
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
                    selectedServices = listOf(service.serviceId)
                )
            )
            if (result is Resource.Error && result.message.isSlotConflictMessage()) {
                val refreshedSlots = getBookableSlots(dentist.dentistId, date)
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

    private suspend fun getBookableSlots(dentistId: Int, date: String): Resource<List<AvailableSlotDTO>> {
        val availableSlots = appointmentRepository.getAvailableTimeslots(dentistId, date)
        if (availableSlots !is Resource.Success) return availableSlots
        return Resource.Success(availableSlots.data.distinctBy { it.startTime?.take(5) })
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
            service = state.selectedService?.serviceName.orEmpty(),
            price = state.selectedService?.price?.let { "₱${String.format(java.util.Locale.US, "%,.0f", it)}" }.orEmpty(),
            date = state.selectedDate.orEmpty(),
            time = listOfNotNull(state.selectedSlot?.startTime, state.selectedSlot?.endTime).joinToString(" – "),
            clinic = "Clinexus Dental Clinic",
            patient = listOfNotNull(patient?.firstName, patient?.lastName).joinToString(" ").ifBlank { patient?.email.orEmpty() }
        )
    }
}

private fun String?.isSlotConflictMessage(): Boolean {
    val value = this?.lowercase().orEmpty()
    return value.contains("already booked") || value.contains("no longer available") || value.contains("just booked")
}
