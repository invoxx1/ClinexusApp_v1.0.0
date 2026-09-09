
package com.example.clinexusapp.model

import com.google.gson.annotations.SerializedName

data class GenericResponse(
    val success: Boolean? = null,
    val message: String? = null,
    val verified: Boolean? = null,

    @SerializedName("resetToken")
    val resetToken: String? = null,

    @SerializedName("changePasswordToken")
    val changePasswordToken: String? = null,

    val attemptsRemaining: Int? = null,
    val lockedUntil: String? = null,

    @SerializedName("debug_otp")
    val debugOtp: String? = null,
)

data class NotificationDTO(
    @SerializedName("notification_id") val notificationId: Int,
    @SerializedName("recipient_type") val recipientType: String? = null,
    @SerializedName("notification_type") val notificationType: String? = null,
    val title: String,
    val message: String,
    @SerializedName("reference_type") val referenceType: String? = null,
    @SerializedName("reference_id") val referenceId: Int? = null,
    @SerializedName("is_read") val isRead: Int,
    @SerializedName("created_at") val createdAt: String? = null,
)

