package com.example.clinexusapp.model

import com.google.gson.annotations.SerializedName

data class AppointmentDTO(
    @SerializedName("appointment_id") val appointmentId: Int,
    @SerializedName("patient_id") val patientId: Int,
    @SerializedName("dentist_id") val dentistId: Int,
    @SerializedName("appointment_date") val appointmentDate: String,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String,
    @SerializedName("appointment_type") val appointmentType: String,
    @SerializedName("appointment_status") val appointmentStatus: String,
    @SerializedName("notes") val notes: String?,
    @SerializedName("dentist_first_name") val dentistFirstName: String?,
        @SerializedName("dentist_last_name") val dentistLastName: String?,
        @SerializedName(value = "dentist_specialty", alternate = ["specialization_name", "specialization"]) val dentistSpecialty: String? = null,
        @SerializedName(value = "dentist_profile_image", alternate = ["profile_image"]) val dentistProfileImage: String? = null,
        @SerializedName(value = "service_name", alternate = ["appointment_service"]) val serviceName: String? = null,
        @SerializedName("price") val price: Double? = null,
        @SerializedName(value = "clinic_name", alternate = ["branch_name"]) val clinicName: String? = null,
        @SerializedName(value = "submitted_at", alternate = ["created_at"]) val submittedAt: String? = null,
        @SerializedName("cancelled_by") val cancelledBy: String? = null,
        @SerializedName("cancelled_at") val cancelledAt: String? = null,
        @SerializedName("cancellation_reason") val cancellationReason: String? = null,
        @SerializedName("reschedule_note") val rescheduleNote: String? = null,
        @SerializedName("requested_date") val requestedDate: String? = null,
        @SerializedName("requested_start_time") val requestedStartTime: String? = null,
        @SerializedName("requested_end_time") val requestedEndTime: String? = null,
        @SerializedName("reschedule_source") val rescheduleSource: String? = null
) {
    // ✅ Combine first and last names
    val doctor: String get() = listOfNotNull(dentistFirstName, dentistLastName)
        .joinToString(" ")
        .ifEmpty { "Unknown Dentist" }

    // ✅ Use the correct field name
    val treatment: String get() = appointmentType.replaceFirstChar { it.uppercase() }

    val needsPatientScheduleChoice: Boolean
        get() = appointmentStatus.equals("needs_reschedule", ignoreCase = true) && requestedDate.isNullOrBlank()

    // ✅ Format status for display
    val displayStatus: String get() = when (appointmentStatus.lowercase().trim()) {
        "needs_cancellation" -> "Pending Cancellation"
        "needs_reschedule" -> "Pending Reschedule"
        "no_show" -> "No Show"
        "pending" -> "Pending"
        "requested" -> "Requested"
        "scheduled" -> "Scheduled"
        "confirmed" -> "Confirmed"
        "cancelled", "canceled" -> "Cancelled"
        "completed", "done" -> "Completed"
        "rejected", "declined", "denied" -> "Rejected"
        else -> appointmentStatus.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }
}
data class ChatMessageDTO(
    val id: String,
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long,
    val isImage: Boolean = false
)

data class ClinicNewsDTO(
    @SerializedName("id") val id: String?,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("date") val date: String
)

data class HealthInsightDTO(
    @SerializedName("id") val id: String?,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("category") val category: String,
    @SerializedName("iconEmoji") val iconEmoji: String
)
