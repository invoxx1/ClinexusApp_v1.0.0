package com.example.clinexusapp.model

import com.google.gson.annotations.SerializedName

data class ChangePasswordRequest(
    @SerializedName("changePasswordToken")
    val changePasswordToken: String,

    @SerializedName("newPassword")
    val password: String
)