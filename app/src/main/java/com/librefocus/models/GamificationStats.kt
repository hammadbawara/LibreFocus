package com.librefocus.models

/**
 * Domain model for gamification statistics.
 * Represents user's overall progress in the gamification system.
 */
data class GamificationStats(
    val totalPoints: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActiveDate: Long = 0L,
    val unlockedBadgeCount: Int = 0,
    val completedChallengeCount: Int = 0
)

