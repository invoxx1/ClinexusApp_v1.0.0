package com.example.clinexusapp.model

import com.google.gson.annotations.SerializedName

data class MessageDetailDTO(
    @SerializedName("message_id") val messageId: Int,
    @SerializedName("account_id") val accountId: Int,
    @SerializedName("account_type") val accountType: String,
    @SerializedName("message_content") val messageContent: String,
    @SerializedName("message_date") val messageDate: String? = null,
    @SerializedName("message_time") val messageTime: String,
    @SerializedName("is_read") val isRead: Boolean = false,
    
    // Flattened attachment fields from backend
    @SerializedName("attachment_id") val attachmentId: Int? = null,
    @SerializedName("file_url") val fileUrl: String? = null,
    @SerializedName("download_url") val downloadUrl: String? = null,
    @SerializedName("file_name") val fileName: String? = null,
    @SerializedName("original_file_name") val originalFileName: String? = null,
    @SerializedName("file_type") val fileType: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null,

    // Keep for potential future use or if backend changes
    val attachments: List<AttachmentDTO>? = null,
)
