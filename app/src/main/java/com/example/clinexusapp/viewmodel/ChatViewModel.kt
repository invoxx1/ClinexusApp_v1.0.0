package com.example.clinexusapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clinexusapp.model.*
import com.example.clinexusapp.repository.ChatRepository
import com.example.clinexusapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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

    private val _actionMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val actionMessages: SharedFlow<String> = _actionMessages.asSharedFlow()

    private val _conversationSearchMatches = MutableStateFlow<Map<Int, String>>(emptyMap())
    val conversationSearchMatches: StateFlow<Map<Int, String>> = _conversationSearchMatches.asStateFlow()
    private val _conversationSearchLoading = MutableStateFlow(false)
    val conversationSearchLoading: StateFlow<Boolean> = _conversationSearchLoading.asStateFlow()
    private val messageHistoryCache = mutableMapOf<Int, List<MessageDetailDTO>>()
    private var searchJob: kotlinx.coroutines.Job? = null

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
        stopPollingMessages()
        _selectedContact.value = contact
        _selectedConversation.value = null
        _conversationMessagesState.value = Resource.Idle
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
            
            if (result is Resource.Success) {
                val existingConversation = _selectedConversation.value
                if (existingConversation != null) {
                    fetchConversationMessages(existingConversation.conversationId)
                } else {
                    val contact = _selectedContact.value
                    val conversationsResult = repository.getConversations()
                    _conversationsState.value = conversationsResult

                    val createdConversation = (conversationsResult as? Resource.Success)?.data
                        ?.firstOrNull { conversation ->
                            conversation.conversationId == result.data.conversationId ||
                                (contact != null &&
                                    conversation.accountId == contact.accountId &&
                                    conversation.accountType.equals(contact.accountType, ignoreCase = true))
                        }
                        ?: if (contact != null && result.data.conversationId != null) {
                            ConversationDTO(
                                conversationId = result.data.conversationId,
                                accountId = contact.accountId,
                                accountType = contact.accountType,
                                name = contact.name,
                                role = contact.role,
                                lastMessage = messageContent,
                                lastMessageTime = null,
                                lastMessageId = result.data.messageId,
                                profilePicture = contact.profilePicture,
                            )
                        } else null

                    if (createdConversation != null) {
                        _selectedConversation.value = createdConversation
                        _selectedContact.value = null
                        fetchConversationMessages(createdConversation.conversationId)
                        startPollingMessages(createdConversation.conversationId)
                    }
                }
                if (existingConversation != null) fetchConversations()
            } else if (result is Resource.Error) {
                _actionMessages.emit(result.message ?: "Message was not sent. Please try again.")
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

    fun searchConversationHistory(query: String, conversations: List<ConversationDTO>) {
        searchJob?.cancel()
        val terms = query.trim().split(Regex("\\s+")).filter(String::isNotBlank)
        if (terms.isEmpty()) {
            _conversationSearchMatches.value = emptyMap()
            _conversationSearchLoading.value = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(300)
            _conversationSearchLoading.value = true
            try {
                val histories = coroutineScope {
                    conversations.map { conversation ->
                        async {
                            val cached = messageHistoryCache[conversation.conversationId]
                            val messages = cached ?: when (val result = repository.getConversationMessages(conversation.conversationId)) {
                                is Resource.Success -> result.data.messages.also {
                                    messageHistoryCache[conversation.conversationId] = it
                                }
                                else -> emptyList()
                            }
                            conversation.conversationId to messages
                        }
                    }.awaitAll()
                }

                _conversationSearchMatches.value = histories.mapNotNull { (conversationId, messages) ->
                    val match = messages.lastOrNull { message ->
                        terms.all { term -> message.messageContent.contains(term, ignoreCase = true) }
                    }
                    match?.let { conversationId to it.messageContent }
                }.toMap()
            } finally {
                _conversationSearchLoading.value = false
            }
        }
    }

    fun deleteMessage(messageId: Int) {
        viewModelScope.launch {
            when (val result = repository.deleteMessage(messageId)) {
                is Resource.Success -> {
                    _selectedConversation.value?.let { fetchConversationMessages(it.conversationId) }
                    fetchConversations()
                    _actionMessages.emit("Message deleted")
                }
                is Resource.Error -> _actionMessages.emit(result.message ?: "Could not delete the message. Please try again.")
                else -> Unit
            }
        }
    }

    fun deleteConversation(conversationId: Int) {
        viewModelScope.launch {
            when (val result = repository.deleteConversation(conversationId)) {
                is Resource.Success -> {
                    if (_selectedConversation.value?.conversationId == conversationId) selectConversation(null)
                    fetchConversations()
                    _actionMessages.emit("Conversation deleted")
                }
                is Resource.Error -> _actionMessages.emit(result.message ?: "Could not delete the conversation. Please try again.")
                else -> Unit
            }
        }
    }
}
