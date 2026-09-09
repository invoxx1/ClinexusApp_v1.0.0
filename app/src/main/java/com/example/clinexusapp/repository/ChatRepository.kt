package com.example.clinexusapp.repository

import com.example.clinexusapp.api.ApiService
import com.example.clinexusapp.model.*
import com.example.clinexusapp.util.Resource
import com.example.clinexusapp.util.SessionManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

interface ChatRepository {
    suspend fun getAvailableContacts(): Resource<List<ContactDTO>>
    suspend fun getConversations(): Resource<List<ConversationDTO>>
    suspend fun getConversationMessages(conversationId: Int): Resource<ConversationMessagesResponse>
    suspend fun sendMessage(
        receiverAccountType: String,
        receiverAccountID: Int,
        messageContent: String,
        conversationID: Int?,
        attachmentPart: MultipartBody.Part?
    ): Resource<SendMessageResponse>
    suspend fun markConversationAsRead(conversationId: Int, lastReadMessageID: Int): Resource<Unit>
}

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : ChatRepository {

    private val authHeader get() = "Bearer ${SessionManager.token}"

    override suspend fun getAvailableContacts(): Resource<List<ContactDTO>> {
        return try {
            val response = apiService.getAvailableContacts(authHeader)
            if (response.isSuccessful) {
                Resource.Success(response.body() ?: emptyList())
            } else {
                Resource.Error(response.errorBody()?.string() ?: "Error fetching contacts")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    override suspend fun getConversations(): Resource<List<ConversationDTO>> {
        return try {
            val response = apiService.getConversations(authHeader)
            if (response.isSuccessful) {
                Resource.Success(response.body() ?: emptyList())
            } else {
                Resource.Error(response.errorBody()?.string() ?: "Error fetching conversations")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    override suspend fun getConversationMessages(conversationId: Int): Resource<ConversationMessagesResponse> {
        return try {
            val response = apiService.getConversationMessages(authHeader, conversationId)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error(response.errorBody()?.string() ?: "Error fetching messages")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    override suspend fun sendMessage(
        receiverAccountType: String,
        receiverAccountID: Int,
        messageContent: String,
        conversationID: Int?,
        attachmentPart: MultipartBody.Part?
    ): Resource<SendMessageResponse> {
        return try {
            val typePart = receiverAccountType.toRequestBody("text/plain".toMediaTypeOrNull())
            val idPart = receiverAccountID.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val contentPart = messageContent.toRequestBody("text/plain".toMediaTypeOrNull())
            val convPart = conversationID?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            
            val response = apiService.sendMessage(authHeader, typePart, idPart, contentPart, convPart, attachmentPart)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error(response.errorBody()?.string() ?: "Error sending message")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    override suspend fun markConversationAsRead(conversationId: Int, lastReadMessageID: Int): Resource<Unit> {
        return try {
            val request = MarkReadRequest(lastReadMessageID)
            val response = apiService.markConversationAsRead(authHeader, conversationId, request)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error(response.errorBody()?.string() ?: "Error marking as read")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }
}
