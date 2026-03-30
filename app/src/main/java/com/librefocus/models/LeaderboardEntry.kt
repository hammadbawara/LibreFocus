package com.librefocus.models

/**
 * Domain model for a leaderboard entry.
 */
data class LeaderboardEntry(
    val id: Int,
    val userId: String,
    val username: String,
    val points: Int,
    val rank: Int = 0,
    val updatedAt: Long = 0L
)

