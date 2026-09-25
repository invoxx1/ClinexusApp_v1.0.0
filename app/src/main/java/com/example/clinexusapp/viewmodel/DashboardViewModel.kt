package com.example.clinexusapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clinexusapp.api.AppointmentRepository
import com.example.clinexusapp.api.AuthRepository
import com.example.clinexusapp.model.AppointmentDTO
import com.example.clinexusapp.model.ClinicNewsDTO
import com.example.clinexusapp.model.HealthInsightDTO
import com.example.clinexusapp.model.PromotionDTO
import com.example.clinexusapp.model.PatientQueueDTO
import com.example.clinexusapp.util.Resource
import com.example.clinexusapp.util.SessionManager
import com.example.clinexusapp.util.NotificationHelper
import com.example.clinexusapp.util.DateUtils
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val appointmentRepository: AppointmentRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private var queueMonitorJob: Job? = null
    private var monitoredQueueAppointmentId: Int? = null
    private val sentQueueAlerts = mutableSetOf<String>()

    private val _newsState = MutableStateFlow<Resource<List<ClinicNewsDTO>>>(Resource.Loading)
    val newsState = _newsState.asStateFlow()

    private val _insightsState = MutableStateFlow<Resource<List<HealthInsightDTO>>>(Resource.Loading)
    val insightsState = _insightsState.asStateFlow()

    private val _promotionsState = MutableStateFlow<Resource<List<PromotionDTO>>>(Resource.Loading)
    val promotionsState = _promotionsState.asStateFlow()

    private val _nextAppointment = MutableStateFlow<Resource<AppointmentDTO?>>(Resource.Idle)
    val nextAppointment = _nextAppointment.asStateFlow()

    private val _checkingInAppointmentId = MutableStateFlow<Int?>(null)
    val checkingInAppointmentId = _checkingInAppointmentId.asStateFlow()

    private val _checkedInAppointmentIds = MutableStateFlow<Set<Int>>(emptySet())
    val checkedInAppointmentIds = _checkedInAppointmentIds.asStateFlow()

    private val _queueAppointmentId = MutableStateFlow<Int?>(null)
    val queueAppointmentId = _queueAppointmentId.asStateFlow()

    private val _queueStatus = MutableStateFlow<Resource<PatientQueueDTO>>(Resource.Idle)
    val queueStatus = _queueStatus.asStateFlow()

    private val _appointmentActionError = MutableStateFlow<String?>(null)
    val appointmentActionError = _appointmentActionError.asStateFlow()

    private val _unreadNotificationsCount = MutableStateFlow(0)
    val unreadNotificationsCount = _unreadNotificationsCount.asStateFlow()

    fun fetchDashboardData() {
        viewModelScope.launch {
            _newsState.value = Resource.Loading
            _insightsState.value = Resource.Loading
            _promotionsState.value = Resource.Loading
            _nextAppointment.value = Resource.Loading
            
            // Parallel execution
            launch {
                val profileResult = repository.getPatientProfile()
                if (profileResult is Resource.Success) {
                    SessionManager.updateProfile(profileResult.data)
                }
            }

            launch {
                refreshAppointments()
            }

            launch {
                refreshNotifications()
            }

            launch {
                _newsState.value = repository.getClinicNews()
            }

            launch {
                _insightsState.value = repository.getHealthInsights()
            }

            launch {
                _promotionsState.value = appointmentRepository.getActivePromotions()
            }
        }
    }

    fun refreshAppointmentsAndNotifications() {
        viewModelScope.launch {
            launch { refreshAppointments() }
            launch { refreshNotifications() }
        }
    }

    fun selfCheckIn(appointmentId: Int) {
        if (_checkingInAppointmentId.value != null) return
        viewModelScope.launch {
            _checkingInAppointmentId.value = appointmentId
            when (val result = appointmentRepository.selfCheckIn(appointmentId)) {
                is Resource.Success -> {
                    _checkingInAppointmentId.value = null
                    _checkedInAppointmentIds.value = _checkedInAppointmentIds.value + appointmentId
                    refreshAppointments()
                    loadQueueStatus(appointmentId)
                }
                is Resource.Error -> {
                    _checkingInAppointmentId.value = null
                    _appointmentActionError.value = result.message ?: "Unable to check in."
                }
                else -> Unit
            }
        }
    }

    fun loadQueueStatus(appointmentId: Int) {
        _queueAppointmentId.value = appointmentId
        _queueStatus.value = Resource.Loading
        if (monitoredQueueAppointmentId == appointmentId && queueMonitorJob?.isActive == true) return
        queueMonitorJob?.cancel()
        monitoredQueueAppointmentId = appointmentId
        queueMonitorJob = viewModelScope.launch {
            while (monitoredQueueAppointmentId == appointmentId) {
                val result = appointmentRepository.getPatientQueueStatus(appointmentId)
                _queueStatus.value = result
                if (result is Resource.Success) {
                    notifyQueueChange(appointmentId, result.data)
                    if (result.data.queueStatus.lowercase() in setOf("in_progress", "completed", "cancelled")) {
                        monitoredQueueAppointmentId = null
                        break
                    }
                }
                delay(10_000)
            }
        }
    }

    private fun notifyQueueChange(appointmentId: Int, queue: PatientQueueDTO) {
        val status = queue.queueStatus.lowercase()
        val alert = when {
            status == "ready" -> "ready" to ("You're next" to "Please stay nearby. The clinic is ready for you.")
            queue.patientsAhead == 1 -> "almost" to ("Almost your turn" to "There is only one patient ahead of you.")
            else -> return
        }
        if (sentQueueAlerts.add("$appointmentId:${alert.first}")) {
            NotificationHelper.showNotification(context, alert.second.first, alert.second.second, appointmentId)
        }
    }

    fun closeQueueStatus() {
        _queueAppointmentId.value = null
        _queueStatus.value = Resource.Idle
    }

    fun clearAppointmentActionError() {
        _appointmentActionError.value = null
    }

    override fun onCleared() {
        queueMonitorJob?.cancel()
        super.onCleared()
    }

    private suspend fun refreshAppointments() {
        val result = appointmentRepository.getPatientAppointments()
        if (result is Resource.Success) {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val now = Date()
            val todayStr = java.time.LocalDate.now(BookingRules.clinicZone).toString()
            val activeAppts = result.data.filter {
                when (mapAppointmentStatus(it.appointmentStatus)) {
                    AppointmentStatus.PENDING,
                    AppointmentStatus.CONFIRMED,
                    AppointmentStatus.IN_PROGRESS,
                    AppointmentStatus.RESCHEDULE_REQUESTED,
                    AppointmentStatus.CANCELLATION_REQUESTED -> true
                    else -> false
                }
            }.filter {
                try {
                    val cleanDate = DateUtils.appointmentDateOnly(it.appointmentDate)
                    val apptDate = sdf.parse("$cleanDate ${it.startTime}")
                    apptDate?.after(now) == true || cleanDate == todayStr
                } catch (_: Exception) {
                    false
                }
            }.sortedWith(compareBy<AppointmentDTO> {
                when (mapAppointmentStatus(it.appointmentStatus)) {
                    AppointmentStatus.IN_PROGRESS -> 0
                    AppointmentStatus.CONFIRMED -> 1
                    AppointmentStatus.PENDING -> 2
                    AppointmentStatus.RESCHEDULE_REQUESTED -> 3
                    AppointmentStatus.CANCELLATION_REQUESTED -> 4
                    else -> 5
                }
            }.thenBy {
                try {
                    val cleanDate = DateUtils.appointmentDateOnly(it.appointmentDate)
                    sdf.parse("$cleanDate ${it.startTime}")?.time ?: Long.MAX_VALUE
                } catch (_: Exception) {
                    Long.MAX_VALUE
                }
            })
            _nextAppointment.value = Resource.Success(activeAppts.firstOrNull())
        } else if (result is Resource.Error) {
            _nextAppointment.value = Resource.Error(result.message ?: "Failed to load appointments")
        }
    }

    private suspend fun refreshNotifications() {
        val result = repository.getNotifications()
        if (result is Resource.Success) {
            _unreadNotificationsCount.value = result.data.count { it.isRead == 0 }
        }
    }
}
