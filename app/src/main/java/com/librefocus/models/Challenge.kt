package com.librefocus.models

/**
 * Domain model for a daily challenge.
 */
data class Challenge(
    val id: Int,
    val title: String,
    val description: String,
    val type: ChallengeType,
    val targetValue: Int,
    val currentProgress: Int = 0,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val date: Long,
    val rewardPoints: Int = 100
) {
    val progressPercentage: Float
        get() = if (targetValue > 0) {
            (currentProgress.toFloat() / targetValue.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
}

enum class ChallengeType {
    LIMIT_USAGE,
    REDUCE_USAGE,
    FOCUS_TIME
}

