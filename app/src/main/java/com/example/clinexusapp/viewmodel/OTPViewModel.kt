package com.example.clinexusapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clinexusapp.api.AuthRepository
import com.example.clinexusapp.model.GenericResponse
import com.example.clinexusapp.model.LoginRequest
import com.example.clinexusapp.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OTPViewModel @Inject constructor(private val repository: AuthRepository) : ViewModel() {

    // General OTP operation state
    private val _otpState = MutableStateFlow<Resource<GenericResponse>?>(null)
    val otpState = _otpState.asStateFlow()

    // Stores the token returned after OTP verification (for reset or change password)
    private val _resetToken = MutableStateFlow<String?>(null)
    val resetToken = _resetToken.asStateFlow()
    private val _resendState = MutableStateFlow<Resource<GenericResponse>?>(null)
    val resendState = _resendState.asStateFlow()

    fun resendOtp(email: String, purpose: String) {
        viewModelScope.launch {
            _resendState.value = Resource.Loading
            _resendState.value = when (purpose) {
                "reset" -> repository.forgotPassword(email)
                "change" -> repository.requestPasswordChange()
                "verification" -> repository.resendVerificationEmail(email)
                else -> Resource.Error("Unsupported OTP request")
            }
        }
    }

    fun resetResendState() { _resendState.value = null }

    // ---------- Registration Email Verification ----------
    fun verifyEmail(email: String, otp: String) {
        viewModelScope.launch {
            _otpState.value = Resource.Loading
            _otpState.value = repository.verifyEmail(email, otp)
        }
    }

    // ---------- Forgot Password (logged out) ----------
    fun forgotPassword(email: String) {
        viewModelScope.launch {
            _otpState.value = Resource.Loading
            _otpState.value = repository.forgotPassword(email)
        }
    }

    fun verifyOTP(email: String, otp: String) {
        viewModelScope.launch {
            _otpState.value = Resource.Loading
            val result = repository.verifyOTP(email, otp)
            (result as? Resource.Success)?.let {
                _resetToken.value = it.data.resetToken ?: it.data.changePasswordToken
            }
            _otpState.value = result
        }
    }

    fun resetPassword(resetToken: String, newPassword: String) {
        viewModelScope.launch {
            _otpState.value = Resource.Loading
            _otpState.value = repository.resetPassword(resetToken, newPassword)
        }
    }

    // ---------- Change Password (logged in) ----------
    fun confirmCurrentPassword(email: String, currentPassword: String) {
        viewModelScope.launch {
            _otpState.value = Resource.Loading
            when (val loginResult = repository.login(LoginRequest(email.trim(), currentPassword))) {
                is Resource.Success -> _otpState.value = repository.requestPasswordChange()
                is Resource.Error -> _otpState.value = Resource.Error(
                    if (
                        loginResult.message?.contains("credential", ignoreCase = true) == true ||
                        loginResult.message?.contains("invalid", ignoreCase = true) == true ||
                        loginResult.message?.contains("password", ignoreCase = true) == true
                    ) {
                        "Current password is incorrect"
                    } else loginResult.message ?: "Unable to verify the current password"
                )
                else -> Unit
            }
        }
    }

    fun requestPasswordChange() {
        viewModelScope.launch {
            _otpState.value = Resource.Idle
            _otpState.value = Resource.Loading
            _otpState.value = repository.requestPasswordChange()
        }
    }

    fun verifyPasswordChangeOTP(otp: String) {
        viewModelScope.launch {
            _otpState.value = Resource.Idle
            _otpState.value = Resource.Loading
            val result = repository.verifyPasswordChangeOTP(otp)
            (result as? Resource.Success)?.let {
                _resetToken.value = it.data.changePasswordToken ?: it.data.resetToken
            }
            _otpState.value = result
        }
    }

    fun changePassword(changePasswordToken: String, newPassword: String) {
        viewModelScope.launch {
            _otpState.value = Resource.Loading
            _otpState.value = repository.changePassword(changePasswordToken, newPassword)
        }
    }

    fun resetOtpState() {
        _otpState.value = null
    }

    fun resetState() {
        _otpState.value = null
        _resetToken.value = null
    }
}
