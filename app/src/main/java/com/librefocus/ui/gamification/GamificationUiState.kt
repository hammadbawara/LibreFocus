package com.librefocus.ui.gamification

import com.librefocus.models.AchievementGroup

data class GamificationUiState(
    val currentGoalMinutes: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalPerfectDays: Int = 0,
    val achievementGroups: List<AchievementGroup> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)