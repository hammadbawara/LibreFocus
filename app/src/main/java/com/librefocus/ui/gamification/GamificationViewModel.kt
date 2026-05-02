package com.librefocus.ui.gamification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.librefocus.data.repository.GamificationRepository
import com.librefocus.utils.DateTimeFormatterManager
import com.librefocus.utils.FormattedDateTimePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GamificationViewModel(
    private val gamificationRepository: GamificationRepository,
    dateTimeFormatterManager: DateTimeFormatterManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(GamificationUiState(isLoading = true))
    val uiState: StateFlow<GamificationUiState> = _uiState.asStateFlow()

    val formattedPreferences: StateFlow<FormattedDateTimePreferences?> =
        dateTimeFormatterManager.formattedPreferences
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            gamificationRepository.recalculateGamification()
                .onSuccess { snapshot ->
                    _uiState.value = GamificationUiState(
                        currentGoalMinutes = snapshot.currentGoalMinutes,
                        currentStreak = snapshot.currentStreak,
                        longestStreak = snapshot.longestStreak,
                        totalPerfectDays = snapshot.totalPerfectDays,
                        totalXp = snapshot.totalXp,
                        level = snapshot.levelProgress.level,
                        xpIntoCurrentLevel = snapshot.levelProgress.xpIntoCurrentLevel,
                        xpForNextLevel = snapshot.levelProgress.xpForNextLevel,
                        xpToNextLevel = snapshot.levelProgress.xpToNextLevel,
                        achievementGroups = snapshot.achievementGroups,
                        latestAchievementAnnouncement = snapshot.latestAchievementAnnouncement,
                        isLoading = false,
                        error = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load gamification data"
                    )
                }
        }
    }

    fun setDailyGoal(minutes: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            gamificationRepository.setDailyScreenTimeGoal(minutes)
                .onSuccess { snapshot ->
                    _uiState.value = GamificationUiState(
                        currentGoalMinutes = snapshot.currentGoalMinutes,
                        currentStreak = snapshot.currentStreak,
                        longestStreak = snapshot.longestStreak,
                        totalPerfectDays = snapshot.totalPerfectDays,
                        totalXp = snapshot.totalXp,
                        level = snapshot.levelProgress.level,
                        xpIntoCurrentLevel = snapshot.levelProgress.xpIntoCurrentLevel,
                        xpForNextLevel = snapshot.levelProgress.xpForNextLevel,
                        xpToNextLevel = snapshot.levelProgress.xpToNextLevel,
                        achievementGroups = snapshot.achievementGroups,
                        latestAchievementAnnouncement = snapshot.latestAchievementAnnouncement,
                        isLoading = false,
                        error = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to update goal"
                    )
                }
        }
    }

        fun acknowledgeAchievementAnnouncement() {
            val announcement = _uiState.value.latestAchievementAnnouncement ?: return
            viewModelScope.launch {
                gamificationRepository.markAchievementAnnouncementSeen(announcement.achievedAtUtc)
                _uiState.value = _uiState.value.copy(latestAchievementAnnouncement = null)
            }
        }
}