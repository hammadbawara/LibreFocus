package com.librefocus.data.local.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Represents a leaderboard entry (local or synced from remote).
 */
@Serializable
@Entity(
    tableName = "leaderboard_entries",
    indices = [
        Index(value = ["userId"], unique = true),
        Index(value = ["points"], unique = false)
    ]
)
data class LeaderboardEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    val userId: String, // Local device ID or Firebase UID
    val username: String,
    val points: Int,
    val rank: Int = 0,
    val updatedAt: Long = 0L // UTC timestamp
)

