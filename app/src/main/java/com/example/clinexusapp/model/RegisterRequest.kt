package com.example.clinexusapp.model

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("middle_name") val middleName: String,
    @SerializedName("last_name") val lastName: String,
    @SerializedName("phone_number") val phoneNumber: String,
    @SerializedName("date_of_birth") val dateOfBirth: String,
    @SerializedName("street_address") val streetAddress: String,
    @SerializedName("province") val province: String,
    @SerializedName("city") val city: String,
    @SerializedName("barangay") val barangay: String
)
