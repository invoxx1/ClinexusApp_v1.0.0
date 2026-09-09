package com.example.clinexusapp.api
import com.example.clinexusapp.model.ChangePasswordRequest
import com.example.clinexusapp.model.ForgotPasswordRequest
import android.util.Log
import com.example.clinexusapp.model.*
import com.example.clinexusapp.util.Resource
import com.example.clinexusapp.util.SessionManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(private val apiService: ApiService) {

    // Helper to convert String to RequestBody for multipart
    private fun String.toPart(): RequestBody =
        this.toRequestBody("text/plain".toMediaTypeOrNull())

    private fun getCleanToken(): String? {
        val raw = SessionManager.token?.trim() ?: return null
        // Sanitize token: Remove surrounding quotes if they exist (common with JSON strings)
        val sanitized = raw.replace("\"", "").trim()
        return if (sanitized.startsWith("Bearer ", ignoreCase = true)) {
            sanitized.substring(7).trim()
        } else {
            sanitized
        }
    }

    private fun getAuthorizationHeader(): String? {
        val clean = getCleanToken() ?: return null
        return "Bearer $clean"
    }

    // ---------- LOGIN ----------
    suspend fun login(request: LoginRequest): Resource<LoginResponse> {
        return try {
            val response = apiService.loginPatient(request)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                val errorMsg = parseError(response.errorBody()?.string())
                Resource.Error(errorMsg ?: "Invalid Credentials")
            }
        } catch (e: IOException) {
            Resource.Error("Could not connect to server. Check your internet connection.")
        } catch (_: HttpException) {
            Resource.Error("Server returned an error. Please try again later.")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unexpected error occurred")
        }
    }

    // ---------- REGISTER ----------
    suspend fun register(request: RegisterRequest, file: MultipartBody.Part? = null): Resource<RegisterResponse> {
        return try {
            val middleNamePart = if (request.middleName.isNullOrBlank()) null else request.middleName.toPart()

            val response = apiService.registerPatient(
                email = request.email.trim().lowercase().toPart(),
                password = request.password.toPart(),
                firstName = request.firstName.toPart(),
                middleName = middleNamePart,
                lastName = request.lastName.toPart(),
                phoneNumber = request.phoneNumber.toPart(),
                dateOfBirth = request.dateOfBirth.toPart(),
                streetAddress = request.streetAddress.toPart(),
                province = request.province.toPart(),
                city = request.city.toPart(),
                barangay = request.barangay.toPart(),
                file = file
            )
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error(parseError(response.errorBody()?.string()) ?: "Registration failed")
            }
        } catch (e: IOException) {
            Resource.Error("Network error: ${e.message}")
        } catch (_: HttpException) {
            Resource.Error("Server error. Please try again later.")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unexpected error")
        }
    }

    // ---------- VERIFY EMAIL ----------
    suspend fun verifyEmail(email: String, otp: String): Resource<GenericResponse> {
        return try {
            val request = VerifyOtpRequest(email.trim().lowercase(), otp)
            val response = apiService.verifyEmail(request)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success == false) {
                    Resource.Error(body.message ?: "OTP verification failed")
                } else {
                    Resource.Success(body)
                }
            } else {
                Resource.Error(parseError(response.errorBody()?.string()) ?: "OTP verification failed")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unexpected error occurred")
        }
    }

    // ---------- VERIFY OTP (for password reset) ----------
    suspend fun verifyOTP(email: String, otp: String): Resource<GenericResponse> {
        return try {
            val request = VerifyOtpRequest(email.trim().lowercase(), otp)
            val response = apiService.verifyOTP(request)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success == false) {
                    Resource.Error(body.message ?: "OTP verification failed")
                } else {
                    Resource.Success(body)
                }
            } else {
                Resource.Error(parseError(response.errorBody()?.string()) ?: "OTP verification failed")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unexpected error occurred")
        }
    }

    // ---------- FORGOT PASSWORD ----------
    suspend fun forgotPassword(email: String): Resource<GenericResponse> {
        return try {
            val cleanEmail = email.trim().lowercase()

            Log.d("FORGOT_PASSWORD", "Sending forgot password request")
            Log.d("FORGOT_PASSWORD", "Email: '$cleanEmail'")

            val request = ForgotPasswordRequest(
                email = cleanEmail
            )

            val response = apiService.forgotPassword(request)

            Log.d("FORGOT_PASSWORD", "HTTP Code: ${response.code()}")
            Log.d("FORGOT_PASSWORD", "Successful: ${response.isSuccessful}")
            Log.d("FORGOT_PASSWORD", "Response: ${response.body()}")

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success == false) {
                    Resource.Error(body.message ?: "Failed to send reset code")
                } else {
                    Resource.Success(body)
                }
            } else {
                val errorBody = response.errorBody()?.string()

                Resource.Error(
                    parseError(errorBody)
                        ?: "Forgot password request failed. HTTP ${response.code()}"
                )
            }

        } catch (e: IOException) {

            Log.e("FORGOT_PASSWORD", "Network error", e)

            Resource.Error(
                "Could not connect to server. Check your internet connection."
            )

        } catch (e: Exception) {

            Log.e("FORGOT_PASSWORD", "Unexpected error", e)

            Resource.Error(
                e.message ?: "An unexpected error occurred"
            )
        }
    }


    // ---------- RESET PASSWORD ----------
    suspend fun resetPassword(resetToken: String, newPassword: String): Resource<GenericResponse> {
        return try {
            val cleanResetToken = resetToken.replace("\"", "").trim()
            val request = ResetPasswordRequest(cleanResetToken, newPassword)
            val response = apiService.resetPassword(request)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success == false) {
                    Resource.Error(body.message ?: "Password reset failed")
                } else {
                    Resource.Success(body)
                }
            } else {
                Resource.Error(parseError(response.errorBody()?.string()) ?: "Password reset failed")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unexpected error occurred")
        }
    }

    // ---------- PASSWORD CHANGE (authenticated) ----------
    suspend fun requestPasswordChange(): Resource<GenericResponse> {
        return try {
            val token = getAuthorizationHeader() ?: return Resource.Error("Not authenticated")
            val response = apiService.requestPasswordChange(token)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success == false) {
                    Resource.Error(body.message ?: "Password change request failed")
                } else {
                    Resource.Success(body)
                }
            } else {
                Resource.Error(parseError(response.errorBody()?.string()) ?: "Password change request failed")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unexpected error occurred")
        }
    }

    suspend fun verifyPasswordChangeOTP(otp: String): Resource<GenericResponse> {
        return try {
            val token = getAuthorizationHeader() ?: return Resource.Error("Not authenticated")
            val request = VerifyPasswordChangeOtpRequest(otp)
            val response = apiService.verifyPasswordChangeOTP(token, request)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success == false) {
                    Resource.Error(body.message ?: "OTP verification failed")
                } else {
                    Resource.Success(body)
                }
            } else {
                Resource.Error(parseError(response.errorBody()?.string()) ?: "OTP verification failed")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unexpected error occurred")
        }
    }

    suspend fun changePassword(changePasswordToken: String, newPassword: String): Resource<GenericResponse> {
        return try {
            val token = getAuthorizationHeader() ?: return Resource.Error("Not authenticated")
            val cleanChangeToken = changePasswordToken.replace("\"", "").trim()
            val request = ChangePasswordRequest(cleanChangeToken, newPassword)
            val response = apiService.changePatientPassword(token, request)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success == false) {
                    Resource.Error(body.message ?: "Password change failed")
                } else {
                    Resource.Success(body)
                }
            } else {
                Resource.Error(parseError(response.errorBody()?.string()) ?: "Password change failed")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unexpected error")
        }
    }

    // ---------- PROFILE ----------
    suspend fun getPatientProfile(): Resource<PatientInfo> {
        return try {
            val token = getAuthorizationHeader() ?: return Resource.Error("Not authenticated")
            val response = apiService.getPatientProfile(token)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error(response.errorBody()?.string() ?: "Failed to fetch profile")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unexpected error occurred")
        }
    }

    suspend fun getNotifications(): Resource<List<NotificationDTO>> {
        return try {
            val token = getAuthorizationHeader() ?: return Resource.Error("Not authenticated")
            val response = apiService.getNotifications(token)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error(parseError(response.errorBody()?.string()) ?: "Failed to fetch notifications")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to fetch notifications")
        }
    }

    suspend fun markNotificationAsRead(notificationId: Int): Resource<GenericResponse> {
        return try {
            val token = getAuthorizationHeader() ?: return Resource.Error("Not authenticated")
            val response = apiService.markNotificationAsRead(token, notificationId)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error(parseError(response.errorBody()?.string()) ?: "Failed to mark notification as read")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to mark notification as read")
        }
    }

    suspend fun markAllNotificationsAsRead(): Resource<GenericResponse> {
        return try {
            val token = getAuthorizationHeader() ?: return Resource.Error("Not authenticated")
            val response = apiService.markAllNotificationsAsRead(token)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error(parseError(response.errorBody()?.string()) ?: "Failed to mark notifications as read")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to mark notifications as read")
        }
    }

    suspend fun updatePatientProfile(
        request: UpdateProfileRequest,
        file: MultipartBody.Part? = null
    ): Resource<GenericResponse> {
        return try {
            val token = getAuthorizationHeader() ?: return Resource.Error("Not authenticated")

            // Sanitize inputs
            val cleanPhone = request.phoneNumber.replace(Regex("[^0-9]"), "")
            val cleanDate = request.dateOfBirth.trim()
            val middlePart = if (request.middleName.isNullOrBlank()) "-".toPart() else request.middleName.toPart()

            if (!cleanDate.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
                return Resource.Error("Date of birth must be in YYYY-MM-DD format")
            }
            if (cleanPhone.length < 10) {
                return Resource.Error("Phone number must have at least 10 digits")
            }

            Log.d("AuthRepository", "Updating profile for: ${request.email}")
            Log.d("AuthRepository", "Date: $cleanDate, Phone: $cleanPhone")

            val response = apiService.updatePatientAccount(
                token = token,
                email = request.email.toPart(),
                firstName = request.firstName.toPart(),
                middleName = middlePart,
                lastName = request.lastName.toPart(),
                phoneNumber = cleanPhone.toPart(),
                dateOfBirth = cleanDate.toPart(),
                streetAddress = request.streetAddress.toPart(),
                province = request.province.toPart(),
                city = request.city.toPart(),
                barangay = request.barangay.toPart(),
                file = file
            )

            if (response.isSuccessful && response.body() != null) {
                Log.d("AuthRepository", "Profile update successful")
                Resource.Success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("AuthRepository", "Update failed: HTTP ${response.code()}, body: $errorBody")
                Resource.Error(parseError(errorBody) ?: "Failed to update profile")
            }
        } catch (e: IOException) {
            Log.e("AuthRepository", "Network error", e)
            Resource.Error("Network error: ${e.message}")
        } catch (e: HttpException) {
            Log.e("AuthRepository", "Server error", e)
            Resource.Error("Server error: ${e.message}")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Unexpected error", e)
            Resource.Error("Unexpected error: ${e.message}")
        }
    }

    // ---------- APPOINTMENTS ----------
    suspend fun getAppointmentHistory(): Resource<List<AppointmentDTO>> {
        return try {
            val token = getAuthorizationHeader() ?: return Resource.Error("Not authenticated")
            val response = apiService.getAppointmentHistory(token)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error(response.errorBody()?.string() ?: "Failed to fetch history")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unexpected error occurred")
        }
    }

    // ---------- CHAT ----------
    suspend fun getConversations(): Resource<List<ConversationDTO>> {
        return try {
            val token = getAuthorizationHeader() ?: return Resource.Error("Not authenticated")
            val response = apiService.getConversations(token)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error(response.errorBody()?.string() ?: "Failed to fetch conversations")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unexpected error")
        }
    }

    suspend fun getAvailableContacts(): Resource<List<ContactDTO>> {
        return try {
            val token = getAuthorizationHeader() ?: return Resource.Error("Not authenticated")
            val response = apiService.getAvailableContacts(token)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error(response.errorBody()?.string() ?: "Failed to fetch contacts")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unexpected error")
        }
    }

    /*
    suspend fun getChatMessages(): Resource<List<ChatMessageDTO>> {
        return try {
            val token = SessionManager.token ?: return Resource.Error("Not authenticated")
            val response = apiService.getChatMessages("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error(response.errorBody()?.string() ?: "Failed to fetch chat messages")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unexpected error")
        }
    }
    */

    suspend fun getConversationMessages(conversationID: Int): Resource<ConversationMessagesResponse> {
        return try {
            val token = getAuthorizationHeader() ?: return Resource.Error("Not authenticated")
            val response = apiService.getConversationMessages(token, conversationID)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error(response.errorBody()?.string() ?: "Failed to fetch conversation messages")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unexpected error")
        }
    }

    suspend fun markConversationAsRead(conversationID: Int, lastMessageId: Int): Resource<Unit> {
        return try {
            val token = getAuthorizationHeader() ?: return Resource.Error("Not authenticated")
            val request = MarkReadRequest(lastMessageId)
            val response = apiService.markConversationAsRead(token, conversationID, request)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error(response.errorBody()?.string() ?: "Failed to mark as read")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unexpected error")
        }
    }

    suspend fun sendMessage(
        receiverAccountType: String,
        receiverAccountID: Int,
        messageContent: String,
        conversationID: Int?,
        attachmentPart: MultipartBody.Part? = null
    ): Resource<SendMessageResponse> {
        return try {
            val token = getAuthorizationHeader() ?: return Resource.Error("Not authenticated")
            val typePart = receiverAccountType.toPart()
            val idPart = receiverAccountID.toString().toPart()
            val contentPart = messageContent.toPart()
            val convPart = conversationID?.toString()?.toPart()

            val response = apiService.sendMessage(token, typePart, idPart, contentPart, convPart, attachmentPart)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error(response.errorBody()?.string() ?: "Failed to send message")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unexpected error")
        }
    }

    // ---------- CLINIC NEWS ----------
    suspend fun getClinicNews(): Resource<List<ClinicNewsDTO>> {
        return try {
            val token = getAuthorizationHeader() ?: return Resource.Error("Not authenticated")
            val response = apiService.getClinicNews(token)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error(response.errorBody()?.string() ?: "Failed to fetch clinic news")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unexpected error")
        }
    }

    // ---------- HEALTH INSIGHTS ----------
    suspend fun getHealthInsights(): Resource<List<HealthInsightDTO>> {
        return try {
            val token = getAuthorizationHeader() ?: return Resource.Error("Not authenticated")
            val response = apiService.getHealthInsights(token)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error(response.errorBody()?.string() ?: "Failed to fetch health insights")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unexpected error")
        }
    }

    // ---------- HELPER ----------
    private fun parseError(errorJson: String?): String? {
        return try {
            val gson = com.google.gson.Gson()
            val errorBody = gson.fromJson(errorJson, GenericResponse::class.java)
            errorBody.message
        } catch (e: Exception) {
            null
        }
    }
}
