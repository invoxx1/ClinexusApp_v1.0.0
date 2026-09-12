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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

    fun loadAddressOptionsForProfile(selectedProvince: String, selectedCity: String) {
        viewModelScope.launch {
            val regionResult = addressRepository.getRegions()
            val regionList = (regionResult as? Resource.Success)?.data ?: _regions.value
            if (regionList.isEmpty()) return@launch
            _regions.value = regionList

            val allProvinces = coroutineScope {
                regionList.map { region ->
                    async {
                        (addressRepository.getProvinces(region.code) as? Resource.Success)?.data.orEmpty()
                    }
                }.awaitAll().flatten()
            }.distinctBy { it.code }
            _provinces.value = allProvinces

            val province = allProvinces.firstOrNull {
                it.displayName.equals(selectedProvince.trim(), ignoreCase = true)
            } ?: return@launch
            val cityList = (addressRepository.getCities(province.code) as? Resource.Success)?.data ?: return@launch
            _cities.value = cityList

            val city = cityList.firstOrNull {
                it.displayName.equals(selectedCity.trim(), ignoreCase = true)
            } ?: return@launch
            (addressRepository.getBarangays(city.code) as? Resource.Success)?.data?.let {
                _barangays.value = it
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
