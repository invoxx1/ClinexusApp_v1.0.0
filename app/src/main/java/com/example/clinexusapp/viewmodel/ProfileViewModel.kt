package com.example.clinexusapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clinexusapp.api.AuthRepository
import com.example.clinexusapp.api.AddressRepository
import com.example.clinexusapp.model.*
import com.example.clinexusapp.util.Resource
import com.example.clinexusapp.util.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val addressRepository: AddressRepository,
) : ViewModel() {

    // Update state
    private val _updateState = MutableStateFlow<Resource<GenericResponse>?>(null)
    val updateState = _updateState.asStateFlow()

    // Address dropdown data
    private val _regions = MutableStateFlow<List<Region>>(emptyList())

    private val _provinces = MutableStateFlow<List<Province>>(emptyList())
    val provinces = _provinces.asStateFlow()

    private val _cities = MutableStateFlow<List<City>>(emptyList())
    val cities = _cities.asStateFlow()

    private val _barangays = MutableStateFlow<List<Barangay>>(emptyList())
    val barangays = _barangays.asStateFlow()

    init {
        loadRegions()
    }

    // ---------- Address Helpers ----------
    private fun loadRegions() {
        viewModelScope.launch {
            (addressRepository.getRegions() as? Resource.Success)?.let {
                _regions.value = it.data
            }
        }
    }

    fun onProvinceSelected(provinceCode: String) {
        viewModelScope.launch {
            _cities.value = emptyList()
            _barangays.value = emptyList()
            (addressRepository.getCities(provinceCode) as? Resource.Success)?.let {
                _cities.value = it.data
            }
        }
    }

    fun onCitySelected(cityCode: String) {
        viewModelScope.launch {
            _barangays.value = emptyList()
            (addressRepository.getBarangays(cityCode) as? Resource.Success)?.let {
                _barangays.value = it.data
            }
        }
    }

    // ---------- Update Profile ----------

    fun updateProfile(
        firstName: String,
        lastName: String,
        email: String,
    ) {
        viewModelScope.launch {
            val currentUser = SessionManager.currentUser.value

            if (currentUser == null) {
                _updateState.value = Resource.Error("User profile not found")
                return@launch
            }

            val request = UpdateProfileRequest(
                email = email.trim(),
                firstName = firstName.trim(),
                middleName = currentUser.middleName,
                lastName = lastName.trim(),
                phoneNumber = currentUser.phoneNumber ?: "",
                dateOfBirth = currentUser.dateOfBirth ?: "",
                streetAddress = currentUser.streetAddress ?: "",
                province = currentUser.province ?: "",
                city = currentUser.city ?: "",
                barangay = currentUser.barangay ?: "",
            )

            _updateState.value = Resource.Loading

            val result = repository.updatePatientProfile(request)

            (result as? Resource.Success)?.let {
                refreshProfile()
            }

            _updateState.value = result
        }
    }

    fun updateFullProfile(
        request: UpdateProfileRequest,
        profileImage: MultipartBody.Part? = null
    ) {
        viewModelScope.launch {
            _updateState.value = Resource.Loading
            val result = repository.updatePatientProfile(request, profileImage)

            (result as? Resource.Success)?.let {
                refreshProfile()
            }

            _updateState.value = result
        }
    }

// ---------- Fetch Profile ----------

    fun fetchProfile() {
        viewModelScope.launch {
            refreshProfile()
        }
    }
    private suspend fun refreshProfile() {
        (repository.getPatientProfile() as? Resource.Success)?.let {
            SessionManager.updateProfile(it.data)
        }
    }

    fun resetState() {
        _updateState.value = null
    }
}