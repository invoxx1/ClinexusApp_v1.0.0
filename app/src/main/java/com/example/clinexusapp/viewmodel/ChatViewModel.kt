package com.example.clinexusapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clinexusapp.model.*
import com.example.clinexusapp.repository.ChatRepository
import com.example.clinexusapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import kotlin.time.Duration.Companion.seconds
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
) : ViewModel() {

    private val _contactsState = MutableStateFlow<Resource<List<ContactDTO>>>(Resource.Idle)
    val contactsState: StateFlow<Resource<List<ContactDTO>>> = _contactsState.asStateFlow()

    private val _conversationsState = MutableStateFlow<Resource<List<ConversationDTO>>>(Resource.Idle)
    val conversationsState: StateFlow<Resource<List<ConversationDTO>>> = _conversationsState.asStateFlow()

    private val _conversationMessagesState = MutableStateFlow<Resource<ConversationMessagesResponse>>(Resource.Idle)
    val conversationMessagesState: StateFlow<Resource<ConversationMessagesResponse>> = _conversationMessagesState.asStateFlow()

    private val _sendMessageState = MutableStateFlow<Resource<SendMessageResponse>>(Resource.Idle)
    val sendMessageState: StateFlow<Resource<SendMessageResponse>> = _sendMessageState.asStateFlow()

    private val _selectedConversation = MutableStateFlow<ConversationDTO?>(null)
    val selectedConversation: StateFlow<ConversationDTO?> = _selectedConversation.asStateFlow()

    private val _selectedContact = MutableStateFlow<ContactDTO?>(null)
    val selectedContact: StateFlow<ContactDTO?> = _selectedContact.asStateFlow()

    private var pollingJob: kotlinx.coroutines.Job? = null

    init {
        fetchConversations()
        fetchContacts()
    }

    fun selectConversation(conversation: ConversationDTO?) {
        _selectedConversation.value = conversation
        _selectedContact.value = null
        conversation?.conversationId?.let {
            fetchConversationMessages(it)
            startPollingMessages(it)
        } ?: stopPollingMessages()
    }

    fun selectContact(contact: ContactDTO?) {
        _selectedContact.value = contact
        _selectedConversation.value = null
    }

    fun fetchConversations() {
        viewModelScope.launch {
            _conversationsState.value = Resource.Loading
            _conversationsState.value = repository.getConversations()
        }
    }

    fun fetchContacts() {
        viewModelScope.launch {
            _contactsState.value = Resource.Loading
            _contactsState.value = repository.getAvailableContacts()
        }
    }

    fun fetchConversationMessages(conversationId: Int) {
        viewModelScope.launch {
            _conversationMessagesState.value = Resource.Loading
            _conversationMessagesState.value = repository.getConversationMessages(conversationId)
        }
    }

    fun startPollingMessages(conversationId: Int) {
        stopPollingMessages()
        pollingJob = viewModelScope.launch {
            while (true) {
                _conversationMessagesState.value = repository.getConversationMessages(conversationId)
                kotlinx.coroutines.delay(3.seconds)
            }
        }
    }

    fun stopPollingMessages() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun sendMessage(
        receiverAccountType: String,
        receiverAccountID: Int,
        messageContent: String,
        conversationID: Int?,
        attachmentPart: MultipartBody.Part? = null,
    ) {
        viewModelScope.launch {
            _sendMessageState.value = Resource.Loading
            val result = repository.sendMessage(
                receiverAccountType = receiverAccountType,
                receiverAccountID = receiverAccountID,
                messageContent = messageContent,
                conversationID = conversationID,
                attachmentPart = attachmentPart,
            )
            _sendMessageState.value = result
            
            if (result is Resource.Success<*>) {
                // Refresh messages
                _selectedConversation.value?.conversationId?.let {
                    fetchConversationMessages(it)
                }
                // Refresh conversations list to update last message
                fetchConversations()
            }
        }
    }

    fun resetSendMessageState() {
        _sendMessageState.value = Resource.Idle
    }

    fun markConversationAsRead(conversationId: Int, lastReadMessageID: Int) {
        viewModelScope.launch {
            repository.markConversationAsRead(conversationId, lastReadMessageID)
        }
    }
}
