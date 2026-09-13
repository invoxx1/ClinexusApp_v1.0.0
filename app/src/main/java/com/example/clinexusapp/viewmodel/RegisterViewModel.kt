package com.example.clinexusapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clinexusapp.api.AddressRepository
import com.example.clinexusapp.api.AuthRepository
import com.example.clinexusapp.model.*
import com.example.clinexusapp.util.Resource
import com.example.clinexusapp.util.isValidBirthDate
import com.example.clinexusapp.util.isValidPhilippineMobile
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
    private val _addressLoading = MutableStateFlow<String?>(null)
    val addressLoading = _addressLoading.asStateFlow()
    private val _addressError = MutableStateFlow<String?>(null)
    val addressError = _addressError.asStateFlow()
    private var selectedRegionCode: String? = null
    private var selectedProvinceCode: String? = null
    private var selectedCityCode: String? = null

    init {
        loadRegions()
    }

    private fun loadRegions() {
        viewModelScope.launch {
            _addressLoading.value = "Region"
            _addressError.value = null
            val result = addressRepository.getRegions()
            if (result is Resource.Success) {
                _regions.value = result.data
            } else if (result is Resource.Error) {
                _addressError.value = result.message ?: "Unable to load regions"
            }
            _addressLoading.value = null
        }
    }

    fun onRegionSelected(regionCode: String) {
        selectedRegionCode = regionCode
        _provinces.value = emptyList()
        _cities.value = emptyList()
        _barangays.value = emptyList()
        viewModelScope.launch {
            _addressLoading.value = "Province"
            _addressError.value = null
            val result = addressRepository.getProvinces(regionCode)
            if (result is Resource.Success) {
                _provinces.value = result.data
            } else if (result is Resource.Error) {
                _addressError.value = result.message ?: "Unable to load provinces"
            }
            _addressLoading.value = null
        }
    }

    fun onProvinceSelected(provinceCode: String) {
        selectedProvinceCode = provinceCode
        _cities.value = emptyList()
        _barangays.value = emptyList()
        viewModelScope.launch {
            _addressLoading.value = "City / Municipality"
            _addressError.value = null
            val result = addressRepository.getCities(provinceCode)
            if (result is Resource.Success) {
                _cities.value = result.data
            } else if (result is Resource.Error) {
                _addressError.value = result.message ?: "Unable to load cities"
            }
            _addressLoading.value = null
        }
    }

    fun onCitySelected(cityCode: String) {
        selectedCityCode = cityCode
        _barangays.value = emptyList()
        viewModelScope.launch {
            _addressLoading.value = "Barangay"
            _addressError.value = null
            val result = addressRepository.getBarangays(cityCode)
            if (result is Resource.Success) {
                _barangays.value = result.data
            } else if (result is Resource.Error) {
                _addressError.value = result.message ?: "Unable to load barangays"
            }
            _addressLoading.value = null
        }
    }

    fun retryAddress(level: String) {
        when (level) {
            "Region" -> loadRegions()
            "Province" -> selectedRegionCode?.let(::onRegionSelected)
            "City / Municipality" -> selectedProvinceCode?.let(::onProvinceSelected)
            "Barangay" -> selectedCityCode?.let(::onCitySelected)
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

        if (!isValidPhilippineMobile(phoneNumber)) {
            _validationError.value = "Enter a Philippine mobile number such as 09171234567"
            return
        }

        if (!isValidBirthDate(dateOfBirth)) {
            _validationError.value = "Choose a valid date of birth"
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _validationError.value = "Please enter a valid email address"
            return
        }

        if (!com.example.clinexusapp.util.isValidNewPassword(password)) {
            _validationError.value = "Use at least 8 characters, at least 1 uppercase letter, and no spaces"
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
