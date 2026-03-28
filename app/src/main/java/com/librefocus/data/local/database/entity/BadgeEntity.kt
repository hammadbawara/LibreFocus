package com.librefocus.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Represents a badge/achievement that users can unlock.
 */
@Serializable
@Entity(tableName = "badges")
data class BadgeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    val title: String,
    val description: String,
    val icon: String, // Drawable resource name
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null, // UTC timestamp
    val type: String // Badge category (streak, focus, limit, etc.)
)

