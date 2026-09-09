package com.example.clinexusapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clinexusapp.api.AddressRepository
import com.example.clinexusapp.api.AuthRepository
import com.example.clinexusapp.model.*
import com.example.clinexusapp.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val addressRepository: AddressRepository,
) : ViewModel() {

    private val _registerState = MutableStateFlow<Resource<RegisterResponse>?>(null)
    val registerState = _registerState.asStateFlow()

    private val _validationError = MutableStateFlow<String?>(null)
    val validationError = _validationError.asStateFlow()

    // Address State
    private val _regions = MutableStateFlow<List<Region>>(emptyList())
    val regions = _regions.asStateFlow()

    private val _provinces = MutableStateFlow<List<Province>>(emptyList())
    val provinces = _provinces.asStateFlow()

    private val _cities = MutableStateFlow<List<City>>(emptyList())
    val cities = _cities.asStateFlow()

    private val _barangays = MutableStateFlow<List<Barangay>>(emptyList())
    val barangays = _barangays.asStateFlow()

    init {
        loadRegions()
    }

    private fun loadRegions() {
        viewModelScope.launch {
            val result = addressRepository.getRegions()
            if (result is Resource.Success) {
                _regions.value = result.data
            }
        }
    }

    fun onRegionSelected(regionCode: String) {
        _provinces.value = emptyList()
        _cities.value = emptyList()
        _barangays.value = emptyList()
        viewModelScope.launch {
            val result = addressRepository.getProvinces(regionCode)
            if (result is Resource.Success) {
                _provinces.value = result.data
            }
        }
    }

    fun onProvinceSelected(provinceCode: String) {
        _cities.value = emptyList()
        _barangays.value = emptyList()
        viewModelScope.launch {
            val result = addressRepository.getCities(provinceCode)
            if (result is Resource.Success) {
                _cities.value = result.data
            }
        }
    }

    fun onCitySelected(cityCode: String) {
        _barangays.value = emptyList()
        viewModelScope.launch {
            val result = addressRepository.getBarangays(cityCode)
            if (result is Resource.Success) {
                _barangays.value = result.data
            }
        }
    }

    fun register(
        email: String,
        password: String,
        confirmPassword: String,
        firstName: String,
        middleName: String,
        lastName: String,
        phoneNumber: String,
        dateOfBirth: String,
        streetAddress: String,
        province: String,
        city: String,
        barangay: String,
        profileImage: MultipartBody.Part? = null
    ) {
        val missingField = when {
            firstName.isEmpty() -> "First Name"
            lastName.isEmpty() -> "Last Name"
            email.isEmpty() -> "Email Address"
            password.isEmpty() -> "Password"
            phoneNumber.isEmpty() -> "Phone Number"
            dateOfBirth.isEmpty() -> "Date of Birth"
            streetAddress.isEmpty() -> "Street Address"
            province.isEmpty() -> "Province"
            city.isEmpty() -> "City"
            barangay.isEmpty() -> "Barangay"
            else -> null
        }

        if (missingField != null) {
            _validationError.value = "$missingField is required"
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _validationError.value = "Please enter a valid email address"
            return
        }

        if (password.length < 6) {
            _validationError.value = "Password must be at least 6 characters"
            return
        }

        if (password != confirmPassword) {
            _validationError.value = "Passwords do not match"
            return
        }

        _validationError.value = null
        viewModelScope.launch {
            _registerState.value = Resource.Loading
            val request = RegisterRequest(
                email, password, firstName, middleName, lastName, phoneNumber, dateOfBirth,
                streetAddress, province, city, barangay
            )
            _registerState.value = repository.register(request, profileImage)
        }
    }

    fun resetState() {
        _registerState.value = null
        _validationError.value = null
    }
}
