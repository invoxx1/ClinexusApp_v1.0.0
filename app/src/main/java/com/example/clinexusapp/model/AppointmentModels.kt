package com.example.clinexusapp.model

import com.google.gson.annotations.SerializedName

data class DentistDTO(
    @SerializedName("dentist_id")
    val dentistId: Int,

    @SerializedName("dentist_name")
    val dentistName: String,

    @SerializedName("specialization_name")
    val specializationName: String?,

    @SerializedName("profile_image")
    val profileImage: String? = null,

    @SerializedName("days_of_week")
    val daysOfWeek: String? = null
)

data class AvailableDentistsResponse(
    val success: Boolean,
    val dentists: List<ActiveDentistDTO> = emptyList(),
    val message: String? = null
)

data class ActiveDentistDTO(
    @SerializedName("dentist_id")
    val dentistId: Int,

    @SerializedName("dentist_name")
    val dentistName: String,

    @SerializedName("specialization")
    val specialization: String?,

    @SerializedName("profile_image")
    val profileImage: String? = null,

    @SerializedName("days_of_week")
    val daysOfWeek: String? = null
)

data class BookableServiceDTO(
    @SerializedName("service_id")
    val serviceId: Int,

    @SerializedName("service_name")
    val serviceName: String? = null,

    @SerializedName("price")
    val price: Double? = null,

    @SerializedName("service_category_name")
    val serviceCategoryName: String? = null,

    @SerializedName("pricing_unit_name")
    val pricingUnitName: String? = null,

    @SerializedName("is_bookable_online")
    val isBookableOnline: Int? = null,

    @SerializedName("status")
    val status: String? = null,

    @SerializedName(value = "duration_minutes", alternate = ["durationMinutes"])
    val durationMinutes: Int? = null
)

data class PromotionDTO(
    @SerializedName("promotion_id") val promotionId: Int,
    val title: String,
    val description: String? = null,
    @SerializedName("discount_type") val discountType: String? = null,
    @SerializedName("discount_value") val discountValue: Double? = null,
    @SerializedName("start_date") val startDate: String? = null,
    @SerializedName("end_date") val endDate: String? = null
)

data class ActivePromotionsResponse(
    val success: Boolean,
    val promotions: List<PromotionDTO> = emptyList(),
    val message: String? = null
)

data class AvailableSlotDTO(
    @SerializedName("label")
    val label: String? = null,

    @SerializedName("start_time")
    val startTime: String? = null,

    @SerializedName("end_time")
    val endTime: String? = null
)

data class PatientAppointmentsResponse(
    val success: Boolean,
    val appointments: List<AppointmentDTO>
)

data class AvailableTimeslotsResponse(
    @SerializedName("availableSlots")
    val availableSlots: List<AvailableSlotDTO>
)

data class DentistScheduleDTO(
    @SerializedName("dentist_id")
    val dentistId: Int,

    @SerializedName("working_days")
    val workingDays: List<String>,

    @SerializedName("start_time")
    val startTime: String,

    @SerializedName("end_time")
    val endTime: String
)
data class CreateAppointmentRequest(
    @SerializedName("patientID")
    val patientId: Int,

    @SerializedName("dentistID")
    val dentistId: Int,

    @SerializedName("appointmentDate")
    val appointmentDate: String,

    @SerializedName("startTime")
    val startTime: String,

    @SerializedName("endTime")
    val endTime: String,

    @SerializedName("notes")
    val notes: String,

    @SerializedName("selectedServices")
    val selectedServices: List<Int>
)

data class CreateAppointmentResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("appointment_id")
    val appointmentId: Int? = null,

    @SerializedName(value = "status", alternate = ["appointment_status", "appointmentStatus"])
    val status: String? = null
)

data class RescheduleAppointmentRequest(
    @SerializedName("appointmentDate")
    val appointmentDate: String,

    @SerializedName("startTime")
    val startTime: String,

    @SerializedName("endTime")
    val endTime: String,

    @SerializedName("reschedule_note")
    val note: String,

    @SerializedName("dentistID")
    val dentistId: Int? = null
)

data class CancelAppointmentRequest(
    @SerializedName("cancellation_reason")
    val cancellationNote: String
)