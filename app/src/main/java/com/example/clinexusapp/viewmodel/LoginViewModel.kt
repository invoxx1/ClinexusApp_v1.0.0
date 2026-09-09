package com.example.clinexusapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clinexusapp.api.AuthRepository
import com.example.clinexusapp.model.LoginRequest
import com.example.clinexusapp.model.LoginResponse
import com.example.clinexusapp.util.Resource
import com.example.clinexusapp.util.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(private val repository: AuthRepository) : ViewModel() {

    private val _loginState = MutableStateFlow<Resource<LoginResponse>?>(null)
    val loginState = _loginState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = Resource.Loading
            SessionManager.logout()
            val request = LoginRequest(email, password)
            val result = repository.login(request)
            
            if (result is Resource.Success) {
                val token = result.data.token
                val patient = result.data.patient

                if (token.isNullOrBlank() || patient == null) {
                    _loginState.value = Resource.Error("Login response did not include a valid patient session")
                    return@launch
                }

                SessionManager.saveSession(token, patient)
                
                // Fetch full profile immediately to get first/last name
                val profileResult = repository.getPatientProfile()
                (profileResult as? Resource.Success)?.let {
                    SessionManager.updateProfile(it.data)
                }
            }
            
            _loginState.value = result
        }
    }
    
    fun resetState() {
        _loginState.value = null
    }
}
