package com.librefocus.data.repository

import com.librefocus.data.local.database.dao.ChatMessageDao
import com.librefocus.data.local.database.entity.ChatMessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class ChatRepository(private val chatMessageDao: ChatMessageDao) {

    // Generate a new conversation id
    fun newConversationId(): String = UUID.randomUUID().toString()

    suspend fun saveMessage(conversationId: String, role: String, content: String, timestampMs: Long) = withContext(Dispatchers.IO) {
        val entity = ChatMessageEntity(conversationId = conversationId, role = role, content = content, timestampMs = timestampMs)
        chatMessageDao.insert(entity)
    }

    suspend fun getConversation(conversationId: String): List<ChatMessageEntity> = withContext(Dispatchers.IO) {
        chatMessageDao.getMessagesForConversation(conversationId)
    }

    suspend fun deleteConversation(conversationId: String) = withContext(Dispatchers.IO) {
        chatMessageDao.deleteConversation(conversationId)
    }

    suspend fun getConversationSummaries() = withContext(Dispatchers.IO) {
        chatMessageDao.getConversationSummaries()
    }
}
