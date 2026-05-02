package com.librefocus.ui.gamification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.librefocus.data.repository.GamificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GamificationViewModel(
    private val gamificationRepository: GamificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GamificationUiState(isLoading = true))
    val uiState: StateFlow<GamificationUiState> = _uiState.asStateFlow()

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
                        achievementGroups = snapshot.achievementGroups,
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
                        achievementGroups = snapshot.achievementGroups,
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
}