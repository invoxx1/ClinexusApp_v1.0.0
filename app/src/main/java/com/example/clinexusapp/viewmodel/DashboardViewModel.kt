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

    private val _nextAppointment = MutableStateFlow<AppointmentDTO?>(null)
    val nextAppointment = _nextAppointment.asStateFlow()

    init {
        fetchDashboardData()
    }

    fun fetchDashboardData() {
        viewModelScope.launch {
            _newsState.value = Resource.Loading
            _insightsState.value = Resource.Loading
            _promotionsState.value = Resource.Loading
            
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
                    
                    // Filter: Not cancelled and in the future (or today)
                    // Sort: Closest to current time
                    val next = result.data
                        .asSequence()
                        .filter { it.appointmentStatus.lowercase() != "cancelled" }
                        .sortedBy { 
                            try {
                                val cleanDate = it.appointmentDate.substringBefore("T")
                                sdf.parse("$cleanDate ${it.startTime}")
                            } catch (_: Exception) {
                                null
                            }
                        }
                        .firstOrNull { 
                            try {
                                val cleanDate = it.appointmentDate.substringBefore("T")
                                val apptDate = sdf.parse("$cleanDate ${it.startTime}")
                                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
                                apptDate?.after(now) == true || cleanDate == todayStr
                            } catch (_: Exception) {
                                false
                            }
                        }
                    
                    _nextAppointment.value = next
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
