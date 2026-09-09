package com.example.clinexusapp.model

data class RegisterResponse(
    val success: Boolean? = null,
    val message: String? = null
)

data class PatientProfileResponse(
    val success: Boolean? = null,
    val message: String? = null,
    val patient: PatientInfo? = null
)
