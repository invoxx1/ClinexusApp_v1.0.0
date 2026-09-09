package com.example.clinexusapp.model

import com.google.gson.annotations.SerializedName

data class ContactDTO(
    @SerializedName("accountID") val accountId: Int,
    val name: String,            // Real name
    val role: String,
    @SerializedName("accountType") val accountType: String,
    @SerializedName("profile_image") val profilePicture: String? = null,   // URL to profile image
)
