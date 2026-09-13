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
    private var lastProvinceCode: String? = null
    private var lastCityCode: String? = null

    // ---------- Address Helpers ----------
    fun onProvinceSelected(provinceCode: String) {
        lastProvinceCode = provinceCode
        viewModelScope.launch {
            _cities.value = emptyList()
            _barangays.value = emptyList()
            _addressLoading.value = "City"
            _addressError.value = null
            when (val result = addressRepository.getCities(provinceCode)) {
                is Resource.Success -> _cities.value = result.data
                is Resource.Error -> _addressError.value = result.message ?: "Unable to load cities"
                else -> Unit
            }
            _addressLoading.value = null
        }
    }

    fun onCitySelected(cityCode: String) {
        lastCityCode = cityCode
        viewModelScope.launch {
            _barangays.value = emptyList()
            _addressLoading.value = "Barangay"
            _addressError.value = null
            when (val result = addressRepository.getBarangays(cityCode)) {
                is Resource.Success -> _barangays.value = result.data
                is Resource.Error -> _addressError.value = result.message ?: "Unable to load barangays"
                else -> Unit
            }
            _addressLoading.value = null
        }
    }

    fun retryAddressOptions(level: String) {
        when (level) {
            "Province" -> loadAddressOptionsForProfile(
                SessionManager.currentUser.value?.province.orEmpty(),
                SessionManager.currentUser.value?.city.orEmpty(),
            )
            "City" -> lastProvinceCode?.let(::onProvinceSelected)
            "Barangay" -> lastCityCode?.let(::onCitySelected)
        }
    }

    fun loadAddressOptionsForProfile(selectedProvince: String, selectedCity: String) {
        viewModelScope.launch {
            _addressLoading.value = "Province"
            _addressError.value = null
            val result = addressRepository.getAllProvinces()
            val allProvinces = (result as? Resource.Success)?.data ?: run {
                _addressError.value = (result as? Resource.Error)?.message ?: "Unable to load provinces"
                _addressLoading.value = null
                return@launch
            }
            _provinces.value = allProvinces
            _addressLoading.value = null

            val province = allProvinces.firstOrNull {
                it.displayName.equals(selectedProvince.trim(), ignoreCase = true)
            } ?: return@launch
            lastProvinceCode = province.code
            _addressLoading.value = "City"
            val cityResult = addressRepository.getCities(province.code)
            val cityList = (cityResult as? Resource.Success)?.data ?: run {
                _addressError.value = (cityResult as? Resource.Error)?.message ?: "Unable to load cities"
                _addressLoading.value = null
                return@launch
            }
            _cities.value = cityList
            _addressLoading.value = null

            val city = cityList.firstOrNull {
                it.displayName.equals(selectedCity.trim(), ignoreCase = true)
            } ?: return@launch
            lastCityCode = city.code
            _addressLoading.value = "Barangay"
            when (val barangayResult = addressRepository.getBarangays(city.code)) {
                is Resource.Success -> _barangays.value = barangayResult.data
                is Resource.Error -> _addressError.value = barangayResult.message ?: "Unable to load barangays"
                else -> Unit
            }
            _addressLoading.value = null
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

    fun updateProfilePhoto(profileImage: MultipartBody.Part? = null, remove: Boolean = false) {
        val patient = SessionManager.currentUser.value ?: run {
            _updateState.value = Resource.Error("User profile not found")
            return
        }
        val request = UpdateProfileRequest(
            email = patient.email.orEmpty(),
            firstName = patient.firstName.orEmpty(),
            middleName = patient.middleName,
            lastName = patient.lastName.orEmpty(),
            phoneNumber = patient.phoneNumber.orEmpty(),
            dateOfBirth = patient.dateOfBirth.orEmpty().substringBefore('T'),
            streetAddress = patient.streetAddress.orEmpty(),
            province = patient.province.orEmpty(),
            city = patient.city.orEmpty(),
            barangay = patient.barangay.orEmpty(),
        )
        viewModelScope.launch {
            _updateState.value = Resource.Loading
            val result = repository.updatePatientProfile(request, profileImage, remove)
            if (result is Resource.Success) refreshProfile()
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
