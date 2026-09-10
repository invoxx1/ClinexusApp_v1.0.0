package com.example.clinexusapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clinexusapp.api.AppointmentRepository
import com.example.clinexusapp.api.AuthRepository
import com.example.clinexusapp.model.AppointmentDTO
import com.example.clinexusapp.model.ClinicNewsDTO
import com.example.clinexusapp.model.HealthInsightDTO
import com.example.clinexusapp.model.PromotionDTO
import com.example.clinexusapp.util.Resource
import com.example.clinexusapp.util.SessionManager
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
) : ViewModel() {

    private val _newsState = MutableStateFlow<Resource<List<ClinicNewsDTO>>>(Resource.Loading)
    val newsState = _newsState.asStateFlow()

    private val _insightsState = MutableStateFlow<Resource<List<HealthInsightDTO>>>(Resource.Loading)
    val insightsState = _insightsState.asStateFlow()

    private val _promotionsState = MutableStateFlow<Resource<List<PromotionDTO>>>(Resource.Loading)
    val promotionsState = _promotionsState.asStateFlow()

    private val _nextAppointment = MutableStateFlow<Resource<AppointmentDTO?>>(Resource.Idle)
    val nextAppointment = _nextAppointment.asStateFlow()

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

    private suspend fun refreshAppointments() {
        val result = appointmentRepository.getPatientAppointments()
        if (result is Resource.Success) {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val now = Date()
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
            val activeAppts = result.data.filter {
                when (mapAppointmentStatus(it.appointmentStatus)) {
                    AppointmentStatus.PENDING,
                    AppointmentStatus.CONFIRMED,
                    AppointmentStatus.RESCHEDULE_REQUESTED,
                    AppointmentStatus.CANCELLATION_REQUESTED -> true
                    else -> false
                }
            }.filter {
                try {
                    val cleanDate = it.appointmentDate.substringBefore("T")
                    val apptDate = sdf.parse("$cleanDate ${it.startTime}")
                    apptDate?.after(now) == true || cleanDate == todayStr
                } catch (_: Exception) {
                    false
                }
            }.sortedWith(compareBy<AppointmentDTO> {
                when (mapAppointmentStatus(it.appointmentStatus)) {
                    AppointmentStatus.CONFIRMED -> 0
                    AppointmentStatus.PENDING -> 1
                    AppointmentStatus.RESCHEDULE_REQUESTED -> 2
                    AppointmentStatus.CANCELLATION_REQUESTED -> 3
                    else -> 4
                }
            }.thenBy {
                try {
                    val cleanDate = it.appointmentDate.substringBefore("T")
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
