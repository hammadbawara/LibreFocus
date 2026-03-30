package com.librefocus.ui.gamification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.librefocus.data.repository.GamificationRepository
import com.librefocus.models.Badge
import com.librefocus.models.Challenge
import com.librefocus.models.GamificationStats
import com.librefocus.models.LeaderboardEntry
import com.librefocus.models.Streak
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GamificationUiState(
    val stats: GamificationStats = GamificationStats(),
    val badges: List<Badge> = emptyList(),
    val challenges: List<Challenge> = emptyList(),
    val streaks: List<Streak> = emptyList(),
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class GamificationViewModel(
    private val repository: GamificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GamificationUiState(isLoading = true))
    val uiState: StateFlow<GamificationUiState> = _uiState.asStateFlow()

    init {
        initializeGamification()
        observeGamificationData()
    }

    private fun initializeGamification() {
        viewModelScope.launch {
            try {
                repository.initializeBadges()
                repository.generateTodayChallenge()
                repository.initializeLocalLeaderboard()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    private fun observeGamificationData() {
        viewModelScope.launch {
            repository.getGamificationStats().collect { stats ->
                _uiState.value = _uiState.value.copy(stats = stats)
            }
        }

        viewModelScope.launch {
            repository.getBadges().collect { badges ->
                _uiState.value = _uiState.value.copy(badges = badges)
            }
        }

        viewModelScope.launch {
            repository.getChallenges().collect { challenges ->
                _uiState.value = _uiState.value.copy(challenges = challenges)
            }
        }

        viewModelScope.launch {
            repository.getStreaks().collect { streaks ->
                _uiState.value = _uiState.value.copy(streaks = streaks)
            }
        }

        viewModelScope.launch {
            repository.getLeaderboard().collect { leaderboard ->
                _uiState.value = _uiState.value.copy(leaderboard = leaderboard)
            }
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun refreshGamification() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                repository.syncDailyGamification()
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun claimChallengeReward(challengeId: Int) {
        viewModelScope.launch {
            try {
                repository.completeChallenge(challengeId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun getTodayChallenge(): Challenge? {
        return uiState.value.challenges.firstOrNull()
    }

    fun getUnlockedBadges(): List<Badge> {
        return uiState.value.badges.filter { it.isUnlocked }
    }

    fun getLockedBadges(): List<Badge> {
        return uiState.value.badges.filter { !it.isUnlocked }
    }

    fun getCurrentStreakDays(): Int {
        return uiState.value.stats.currentStreak
    }

    fun getLongestStreakDays(): Int {
        return uiState.value.stats.longestStreak
    }

    fun getTotalPoints(): Int {
        return uiState.value.stats.totalPoints
    }

    fun getUserRank(): Int? {
        return uiState.value.leaderboard.firstOrNull()?.rank
    }
}

