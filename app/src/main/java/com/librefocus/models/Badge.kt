package com.librefocus.models

/**
 * Domain model for a badge/achievement.
 */
data class Badge(
    val id: Int,
    val title: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean,
    val unlockedAt: Long? = null,
    val type: String
)

object BadgeType {
    const val FIRST_STEP = "first_step"
    const val FOCUSED_MIND = "focused_mind"
    const val DISCIPLINE_MASTER = "discipline_master"
    const val WEEK_WARRIOR = "week_warrior"
    const val MONTH_MASTER = "month_master"
    const val LIMIT_ACHIEVED = "limit_achieved"
    const val CHALLENGE_MASTER = "challenge_master"
}

