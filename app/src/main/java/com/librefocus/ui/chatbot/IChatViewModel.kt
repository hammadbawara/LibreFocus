package com.librefocus.ui.chatbot

import kotlinx.coroutines.flow.StateFlow

interface IChatViewModel {
    val messages: StateFlow<List<Message>>
    fun sendMessage(text: String)
    fun setProvider(provider: String)
    fun setModel(model: String)

    // Conversation management
    val conversations: StateFlow<List<ConversationSummary>>
    fun newConversation()
    fun selectConversation(conversationId: String)
    fun refreshConversations()
    fun renameConversation(conversationId: String, newTitle: String)
    fun deleteConversation(conversationId: String)

    // Current selected conversation title (nullable) - UI can show fallback when null
    val currentConversationTitle: StateFlow<String?>
}

// Minimal conversation summary used by UI
data class ConversationSummary(val id: String, val lastMessage: String?, val lastTimestamp: Long?, val title: String? = null)
