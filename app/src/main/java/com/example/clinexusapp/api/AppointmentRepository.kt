package com.example.clinexusapp.api

import android.util.Log
import com.example.clinexusapp.model.*
import com.example.clinexusapp.util.Resource
import com.example.clinexusapp.util.SessionManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppointmentRepository @Inject constructor(
    private val apiService: AppointmentApiService
) {

    companion object {
        private const val TAG = "AppointmentRepository"
    }

    /**
     * Gets the currently stored JWT and guarantees
     * exactly one "Bearer " prefix.
     */
    private fun getAuthorizationHeader(): String? {

        val rawToken =
            SessionManager.token?.trim()
                ?: return null

        if (rawToken.isEmpty()) {
            return null
        }

        val cleanToken =
            if (
                rawToken.startsWith(
                    "Bearer ",
                    ignoreCase = true
                )
            ) {
                rawToken
                    .substring(7)
                    .trim()
            } else {
                rawToken
            }

        if (cleanToken.isEmpty()) {
            return null
        }

        return "Bearer $cleanToken"
    }


    /**
     * Reads backend error messages.
     */
    private fun <T> handleError(
        response: Response<*>,
        defaultMessage: String
    ): Resource<T> {

        val errorBody =
            try {
                response.errorBody()?.string()
                    ?: ""
            } catch (e: Exception) {
                ""
            }

        Log.e(
            TAG,
            "HTTP ${response.code()} error body: $errorBody"
        )

        var serverMessage =
            defaultMessage

        if (
            errorBody.isNotBlank() &&
            errorBody.trim().startsWith("{")
        ) {

            try {

                val type =
                    object :
                        TypeToken<Map<String, Any>>() {}.type

                val map:
                        Map<String, Any> =
                    Gson().fromJson(
                        errorBody,
                        type
                    )

                serverMessage =
                    map["message"]?.toString()
                        ?: map["error"]?.toString()
                                ?: defaultMessage

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Unable to parse server error",
                    e
                )
            }
        }

        val message =
            when (response.code()) {

                400 ->
                    "Bad Request (400): $serverMessage"

                401 ->
                    "Unauthorized (401): $serverMessage. Please login again."

                403 ->
                    "Forbidden (403): $serverMessage. Your session may have expired."

                404 ->
                    "Not Found (404): $serverMessage"

                500 ->
                    "Server Error (500): $serverMessage"

                502 ->
                    "Server temporarily unavailable (502)."

                503 ->
                    "Server temporarily unavailable (503)."

                else ->
                    "Error ${response.code()}: $serverMessage"
            }

        Log.e(
            TAG,
            message
        )

        return Resource.Error(
            message
        )
    }


    // ============================================================
    // ACTIVE DENTISTS
    // ============================================================

    suspend fun getActiveDentists():
            Resource<List<DentistDTO>> {

        return try {

            val token =
                getAuthorizationHeader()

            if (token == null) {

                Log.e(
                    TAG,
                    "getActiveDentists: Token not found"
                )

                return Resource.Error(
                    "Authentication token not found. Please log in again."
                )
            }

            Log.d(
                TAG,
                "GET /api/active-dentists"
            )

            Log.d(
                TAG,
                "Authorization header available"
            )

            val response =
                apiService
                    .getActiveDentists(
                        token
                    )

            Log.d(
                TAG,
                "Dentist HTTP code: ${response.code()}"
            )

            Log.d(
                TAG,
                "Dentist HTTP message: ${response.message()}"
            )

            if (
                response.isSuccessful
            ) {

                val dentists = response.body()?.dentists?.map { dentist ->
                    DentistDTO(
                        dentistId = dentist.dentistId,
                        dentistName = dentist.dentistName,
                        specializationName = dentist.specialization,
                        profileImage = dentist.profileImage,
                        daysOfWeek = dentist.daysOfWeek
                    )
                }

                Log.d(
                    TAG,
                    "Dentist response: $dentists"
                )

                if (dentists != null && response.body()?.success == true) {

                    Log.d(
                        TAG,
                        "Received ${dentists.size} dentists"
                    )

                    Resource.Success(
                        dentists
                    )

                } else {

                    Resource.Error(
                        "Dentist response was empty."
                    )
                }

            } else {

                handleError(
                    response,
                    "Failed to fetch dentists"
                )
            }

        } catch (
            e: java.net.UnknownHostException
        ) {

            Log.e(
                TAG,
                "Cannot connect to server",
                e
            )

            Resource.Error(
                "Cannot connect to server. Check your internet connection."
            )

        } catch (
            e: java.net.SocketTimeoutException
        ) {

            Log.e(
                TAG,
                "Dentist request timed out",
                e
            )

            Resource.Error(
                "Server took too long to respond. Please try again."
            )

        } catch (
            e: Exception
        ) {

            Log.e(
                TAG,
                "Error fetching dentists",
                e
            )

            Resource.Error(
                e.localizedMessage
                    ?: "Network error while fetching dentists."
            )
        }
    }


    // ============================================================
    // DENTIST SCHEDULE
    // ============================================================

    suspend fun getDentistSchedule(
        dentistId: Int
    ): Resource<DentistScheduleDTO> {

        return try {

            val token =
                getAuthorizationHeader()
                    ?: return Resource.Error(
                        "Not authenticated."
                    )

            val response =
                apiService
                    .getDentistSchedule(
                        token,
                        dentistId
                    )

            if (
                response.isSuccessful
            ) {

                val body =
                    response.body()

                if (body != null) {

                    Resource.Success(
                        body
                    )

                } else {

                    Resource.Error(
                        "Empty schedule response."
                    )
                }

            } else {

                handleError(
                    response,
                    "Failed to fetch schedule"
                )
            }

        } catch (
            e: Exception
        ) {

            Log.e(
                TAG,
                "Error fetching dentist schedule",
                e
            )

            Resource.Error(
                e.localizedMessage
                    ?: "Network error while fetching dentist schedule."
            )
        }
    }


    // ============================================================
    // SERVICES
    // ============================================================

    suspend fun getBookableServices():
            Resource<List<BookableServiceDTO>> {

        return try {

            val token =
                getAuthorizationHeader()
                    ?: return Resource.Error(
                        "Authentication token not found. Please log in again."
                    )

            val response =
                apiService
                    .getBookableServices(
                        token
                    )

            if (
                response.isSuccessful
            ) {

                Resource.Success(
                    response.body()
                        ?: emptyList()
                )

            } else {

                handleError(
                    response,
                    "Failed to fetch services"
                )
            }

        } catch (
            e: Exception
        ) {

            Log.e(
                TAG,
                "Error fetching services",
                e
            )

            Resource.Error(
                e.localizedMessage
                    ?: "Network error while fetching services."
            )
        }
    }

    suspend fun getActivePromotions(): Resource<List<PromotionDTO>> {
        return try {
            val token = getAuthorizationHeader()
                ?: return Resource.Error("Not authenticated.")
            val response = apiService.getActivePromotions(token)
            if (response.isSuccessful && response.body()?.success == true) {
                Resource.Success(response.body()?.promotions.orEmpty())
            } else {
                handleError(response, "Failed to fetch promotions")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Network error while fetching promotions.")
        }
    }


    // ============================================================
    // AVAILABLE TIMESLOTS
    // ============================================================

    suspend fun getAvailableTimeslots(
        dentistId: Int,
        date: String
    ): Resource<List<AvailableSlotDTO>> {

        return try {

            val token =
                getAuthorizationHeader()
                    ?: return Resource.Error(
                        "Not authenticated."
                    )

            val response =
                apiService
                    .getAvailableTimeslots(
                        token = token,
                        dentistId = dentistId,
                        appointmentDate = date
                    )

            if (
                response.isSuccessful
            ) {

                val body =
                    response.body()

                Resource.Success(
                    body?.availableSlots
                        ?: emptyList()
                )

            } else {

                handleError(
                    response,
                    "No timeslots available"
                )
            }

        } catch (
            e: Exception
        ) {

            Log.e(
                TAG,
                "Error fetching timeslots",
                e
            )

            Resource.Error(
                e.localizedMessage
                    ?: "Network error while fetching timeslots."
            )
        }
    }


    // ============================================================
    // CREATE APPOINTMENT
    // ============================================================

    suspend fun createAppointment(
        request: CreateAppointmentRequest
    ): Resource<CreateAppointmentResponse> {

        return try {

            val token =
                getAuthorizationHeader()
                    ?: return Resource.Error(
                        "Not authenticated."
                    )

            val response =
                apiService
                    .createAppointment(
                        token,
                        request
                    )

            if (
                response.isSuccessful
            ) {

                val body =
                    response.body()

                if (body != null) {

                    Resource.Success(
                        body
                    )

                } else {

                    Resource.Error(
                        "Empty appointment response."
                    )
                }

            } else {

                handleError(
                    response,
                    "Booking failed"
                )
            }

        } catch (
            e: Exception
        ) {

            Log.e(
                TAG,
                "Error creating appointment",
                e
            )

            Resource.Error(
                e.localizedMessage
                    ?: "Network error while creating appointment."
            )
        }
    }


    // ============================================================
    // PATIENT APPOINTMENTS
    // ============================================================

    suspend fun getPatientAppointments():
            Resource<List<AppointmentDTO>> {

        return try {

            val token =
                getAuthorizationHeader()
                    ?: return Resource.Error(
                        "Not authenticated."
                    )

            val response =
                apiService
                    .getPatientAppointments(
                        token
                    )

            if (
                response.isSuccessful
            ) {

                Resource.Success(
                    response.body()
                        ?.appointments
                        ?: emptyList()
                )

            } else {

                handleError(
                    response,
                    "Failed to fetch appointments"
                )
            }

        } catch (
            e: Exception
        ) {

            Log.e(
                TAG,
                "Error fetching appointments",
                e
            )

            Resource.Error(
                e.localizedMessage
                    ?: "Network error while fetching appointments."
            )
        }
    }


    // ============================================================
    // RESCHEDULE
    // ============================================================

    suspend fun rescheduleAppointment(
        appointmentId: Int,
        request: RescheduleAppointmentRequest
    ): Resource<GenericResponse> {

        return try {

            val token =
                getAuthorizationHeader()
                    ?: return Resource.Error(
                        "Not authenticated."
                    )

            val response =
                apiService
                    .requestReschedule(
                        token = token,
                        appointmentId = appointmentId,
                        request = request
                    )

            if (
                response.isSuccessful
            ) {

                val body =
                    response.body()

                if (body != null) {

                    Resource.Success(
                        body
                    )

                } else {

                    Resource.Error(
                        "Empty response."
                    )
                }

            } else {

                handleError(
                    response,
                    "Reschedule failed"
                )
            }

        } catch (
            e: Exception
        ) {

            Log.e(
                TAG,
                "Error requesting reschedule",
                e
            )

            Resource.Error(
                e.localizedMessage
                    ?: "Network error while requesting reschedule."
            )
        }
    }


    // ============================================================
    // CANCEL
    // ============================================================

    suspend fun cancelAppointment(
        appointmentId: Int,
        request: CancelAppointmentRequest
    ): Resource<GenericResponse> {

        return try {

            val token =
                getAuthorizationHeader()
                    ?: return Resource.Error(
                        "Not authenticated."
                    )

            val response =
                apiService
                    .requestCancelAppointment(
                        token = token,
                        appointmentId = appointmentId,
                        request = request
                    )

            if (
                response.isSuccessful
            ) {

                val body =
                    response.body()

                if (body != null) {

                    Resource.Success(
                        body
                    )

                } else {

                    Resource.Error(
                        "Empty response."
                    )
                }

            } else {

                handleError(
                    response,
                    "Cancellation failed"
                )
            }

        } catch (
            e: Exception
        ) {

            Log.e(
                TAG,
                "Error requesting cancellation",
                e
            )

            Resource.Error(
                e.localizedMessage
                    ?: "Network error while requesting cancellation."
            )
        }
    }
}