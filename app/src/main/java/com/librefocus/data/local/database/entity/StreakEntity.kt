package com.librefocus.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents daily streak tracking data.
 * Each record represents a day when the user met their goal.
 */
@Entity(tableName = "streaks")
data class StreakEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    val date: Long, // Day start UTC
    val goalMet: Boolean = false,
    val screenTimeLimitMinutes: Int = 180, // Default 3 hours
    val actualScreenTimeMinutes: Int = 0
)

