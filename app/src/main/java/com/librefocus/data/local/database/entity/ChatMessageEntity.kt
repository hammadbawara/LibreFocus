package com.librefocus.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "conversation_id") val conversationId: String,
    @ColumnInfo(name = "role") val role: String,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "timestamp_ms") val timestampMs: Long
)

