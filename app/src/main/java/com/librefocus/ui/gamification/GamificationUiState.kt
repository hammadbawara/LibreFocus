package com.librefocus.ui.gamification

import com.librefocus.models.AchievementAnnouncement
import com.librefocus.models.AchievementGroup

data class GamificationUiState(
    val currentGoalMinutes: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalPerfectDays: Int = 0,
    val totalXp: Int = 0,
    val level: Int = 1,
    val xpIntoCurrentLevel: Int = 0,
    val xpForNextLevel: Int = 100,
    val xpToNextLevel: Int = 100,
    val achievementGroups: List<AchievementGroup> = emptyList(),
    val latestAchievementAnnouncement: AchievementAnnouncement? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)