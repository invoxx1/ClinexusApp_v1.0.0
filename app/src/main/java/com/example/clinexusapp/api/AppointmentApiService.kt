package com.example.clinexusapp.api

import com.example.clinexusapp.model.*
import retrofit2.Response
import retrofit2.http.*

interface AppointmentApiService {

    // Active dentists
    @GET("api/active-dentists")
    suspend fun getActiveDentists(
        @Header("Authorization") tokenHeader: String
    ): Response<AvailableDentistsResponse>

    // Dentist schedule
    @GET("api/dentists/{dentistID}/available-slots")
    suspend fun getDentistSchedule(
        @Header("Authorization") token: String,
        @Path("dentistID") dentistId: Int
    ): Response<DentistScheduleDTO>


    @GET("api/patient-appointments")
    suspend fun getPatientAppointments(
        @Header("Authorization") token: String,
    ):  Response<PatientAppointmentsResponse>

    @GET("api/bookable-services")
    suspend fun getBookableServices(
        @Header("Authorization") token: String,
    ): Response<List<BookableServiceDTO>>

    @GET("api/active-promotions-mobile")
    suspend fun getActivePromotions(
        @Header("Authorization") token: String
    ): Response<ActivePromotionsResponse>


    // Dentist available slots
    @GET("api/dentists/{dentistID}/available-slots")
    suspend fun getAvailableTimeslots(
        @Header("Authorization") token: String,
        @Path("dentistID") dentistId: Int,
        @Query("appointmentDate") appointmentDate: String,
    ): Response<AvailableTimeslotsResponse>

    // Create appointment
    @POST("api/appointments")
    suspend fun createAppointment(
        @Header("Authorization") token: String,
        @Body request: CreateAppointmentRequest
    ): Response<CreateAppointmentResponse>

    // Request reschedule
    @PATCH("api/appointments/{appointmentID}/request-reschedule")
    suspend fun requestReschedule(
        @Header("Authorization") token: String,
        @Path("appointmentID") appointmentId: Int,
        @Body request: RescheduleAppointmentRequest
    ): Response<GenericResponse>

    // Request cancellation
    @PATCH("api/appointments/{appointmentID}/request-cancellation")
    suspend fun requestCancelAppointment(
        @Header("Authorization") token: String,
        @Path("appointmentID") appointmentId: Int,
        @Body request: CancelAppointmentRequest
    ): Response<GenericResponse>
}