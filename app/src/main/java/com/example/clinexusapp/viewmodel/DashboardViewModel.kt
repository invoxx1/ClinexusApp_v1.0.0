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

    init {
        fetchDashboardData()
    }

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
                val result = appointmentRepository.getPatientAppointments()
                if (result is Resource.Success) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    val now = Date()
                    
                    val activeAppts = result.data.filter {
                        val status = it.appointmentStatus.lowercase()
                        // Filter out terminal states
                        status != "cancelled" && status != "canceled" && status != "completed" && status != "done" && status != "rejected"
                    }.filter {
                        try {
                            val cleanDate = it.appointmentDate.substringBefore("T")
                            val apptDate = sdf.parse("$cleanDate ${it.startTime}")
                            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
                            // Show today's or future appointments
                            apptDate?.after(now) == true || cleanDate == todayStr
                        } catch (_: Exception) {
                            false
                        }
                    }.sortedWith(compareBy<AppointmentDTO> {
                        // Priority: CONFIRMED (0) > PENDING (1) > RESCHEDULE (2)
                        val status = mapAppointmentStatus(it.appointmentStatus)
                        when (status) {
                            AppointmentStatus.CONFIRMED -> 0
                            AppointmentStatus.PENDING -> 1
                            AppointmentStatus.RESCHEDULE_REQUESTED -> 2
                            else -> 3
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

            launch {
                val notifResult = repository.getNotifications()
                if (notifResult is Resource.Success) {
                    // Only count isRead == 0 as unread
                    val unreadCount = notifResult.data.count { it.isRead == 0 }
                    _unreadNotificationsCount.value = unreadCount
                } else {
                    _unreadNotificationsCount.value = 0
                }
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
}
