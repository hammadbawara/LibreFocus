package com.librefocus.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.librefocus.data.local.database.entity.ChatMessageEntity

@Dao
interface ChatMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity): Long

    @Query("SELECT * FROM chat_messages WHERE conversation_id = :conversationId ORDER BY timestamp_ms ASC")
    suspend fun getMessagesForConversation(conversationId: String): List<ChatMessageEntity>

    @Query("DELETE FROM chat_messages WHERE conversation_id = :conversationId")
    suspend fun deleteConversation(conversationId: String)

    // Conversation summary: last message and timestamp per conversation, ordered by most recent
    data class ConversationSummary(
        val conversation_id: String,
        val lastMessage: String?,
        val lastTimestamp: Long?
    )

    @Query("SELECT cm.conversation_id AS conversation_id, (SELECT content FROM chat_messages WHERE conversation_id = cm.conversation_id ORDER BY timestamp_ms DESC LIMIT 1) AS lastMessage, MAX(timestamp_ms) AS lastTimestamp FROM chat_messages cm GROUP BY cm.conversation_id ORDER BY lastTimestamp DESC")
    suspend fun getConversationSummaries(): List<ConversationSummary>
}
