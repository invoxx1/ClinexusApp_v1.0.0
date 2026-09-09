package com.example.clinexusapp.model

data class ConversationMessagesResponse(
    val messages: List<MessageDetailDTO>,
    val otherParticipantLastReadMessageID: Int?,
    val otherParticipant: ContactDTO? = null,
)
