package com.example.clinexusapp

import com.example.clinexusapp.model.ConversationDTO
import com.example.clinexusapp.ui.screens.chat.searchConversations
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatSearchTest {
    private val conversations = listOf(
        conversation(1, "Maria Santos", "Dentist", "Your appointment is confirmed"),
        conversation(2, "Anna Cruz", "Receptionist", "Please choose another schedule"),
        conversation(3, "Mark Reyes", "Dentist", "Good morning"),
    )

    @Test
    fun `search matches names roles and recent messages`() {
        assertEquals(listOf(1), searchConversations(conversations, "maria").map { it.conversationId })
        assertEquals(listOf(1, 3), searchConversations(conversations, "dentist").map { it.conversationId })
        assertEquals(listOf(2), searchConversations(conversations, "another schedule").map { it.conversationId })
    }

    @Test
    fun `multiple search words may match different conversation fields`() {
        assertEquals(listOf(1), searchConversations(conversations, "maria confirmed").map { it.conversationId })
    }

    private fun conversation(id: Int, name: String, role: String, lastMessage: String) = ConversationDTO(
        conversationId = id,
        accountId = id,
        accountType = "staff",
        name = name,
        role = role,
        lastMessage = lastMessage,
        lastMessageTime = null,
        lastMessageId = id,
        profilePicture = null,
    )
}
