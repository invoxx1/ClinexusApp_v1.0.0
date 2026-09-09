package com.example.clinexusapp.model

import com.google.gson.annotations.SerializedName

data class ConversationDTO(
    @SerializedName("conversation_id") val conversationId: Int,
    @SerializedName("account_id") val accountId: Int,
    @SerializedName("account_type") val accountType: String,
    val name: String,            // Real name of other participant
    val role: String,
    @SerializedName("last_message") val lastMessage: String?,
    @SerializedName("last_message_time") val lastMessageTime: String?,
    @SerializedName("last_message_id") val lastMessageId: Int?,
    @SerializedName("profile_image") val profilePicture: String? = null,   // URL to profile image
)
