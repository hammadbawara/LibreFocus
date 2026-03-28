package com.librefocus.models

/**
 * Domain model for streak tracking.
 */
data class Streak(
    val id: Int,
    val date: Long,
    val goalMet: Boolean,
    val screenTimeLimitMinutes: Int,
    val actualScreenTimeMinutes: Int
)

