package com.librefocus.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents overall gamification statistics for the user.
 * Only one record should exist per user (id = 1).
 */
@Entity(tableName = "gamification_stats")
data class GamificationStatsEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Int = 1,
    
    val totalPoints: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActiveDate: Long = 0L,
    val lastStreakResetDate: Long = 0L
)

