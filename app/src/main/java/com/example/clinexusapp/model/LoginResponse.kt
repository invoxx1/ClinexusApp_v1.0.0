package com.example.clinexusapp.model

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    val message: String? = null,
    val token: String? = null,
    val patient: PatientInfo? = null,
    val success: Boolean? = null
)

data class PatientInfo(
    @SerializedName(value = "account_id", alternate = ["accountID"]) val accountID: Int,
    @SerializedName(value = "patient_id", alternate = ["patientID"]) val patientID: Int,
    val role: String,
    val email: String? = null,
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("middle_name") val middleName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    @SerializedName("phone_number") val phoneNumber: String? = null,
    @SerializedName("date_of_birth") val dateOfBirth: String? = null,
    @SerializedName("street_address") val streetAddress: String? = null,
    @SerializedName("province") val province: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("barangay") val barangay: String? = null,
    @SerializedName("profile_image") val profilePicture: String? = null
)
