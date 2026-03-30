package com.librefocus.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Represents a daily challenge for the user to complete.
 */
@Serializable
@Entity(tableName = "challenges")
data class ChallengeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    val title: String,
    val description: String,
    val type: String, // LIMIT_USAGE, REDUCE_USAGE, FOCUS_TIME
    val targetValue: Int, // In minutes for usage, percentage for reduction, etc.
    val currentProgress: Int = 0, // Current progress towards target
    val isCompleted: Boolean = false,
    val completedAt: Long? = null, // UTC timestamp
    val date: Long, // Date when challenge was created (day start UTC)
    val rewardPoints: Int = 100 // Points awarded for completion
)

