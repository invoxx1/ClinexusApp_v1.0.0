package com.example.clinexusapp.model

import com.google.gson.annotations.SerializedName

data class SendMessageResponse(
    val success: Boolean,
    val message: String,
    @SerializedName("message_id") val messageId: Int? = null,
)
