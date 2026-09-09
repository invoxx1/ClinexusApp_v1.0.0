package com.example.clinexusapp.model

import com.google.gson.annotations.SerializedName

data class AttachmentDTO(
    @SerializedName("attachment_id") val id: Int,
    @SerializedName("file_url") val fileUrl: String,
    @SerializedName("download_url") val downloadUrl: String? = null,
    @SerializedName("file_name") val fileName: String,
    @SerializedName("file_type") val fileType: String,        // e.g., "image/png", "application/pdf"
    @SerializedName("file_size") val fileSize: Long? = null,
)
