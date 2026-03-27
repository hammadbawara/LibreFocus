package com.librefocus.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.librefocus.data.repository.UsageTrackingRepository
import com.librefocus.models.AppUsage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class HomeUiState(
    val apps: List<AppUsage> = emptyList(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val error: String? = null,
    val lastSyncTime: Long? = null,
    val todayScreenTimeMillis: Long = 0L,
    val todayUnlocks: Int = 0
)

class HomeViewModel(
    private val repository: UsageTrackingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadAppUsage()
    }

    fun loadAppUsage() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val apps = getTodayUsage()
                val (screenTimeMillis, unlocks) = getTodaySummary()
                _uiState.value = _uiState.value.copy(
                    apps = apps,
                    isLoading = false,
                    todayScreenTimeMillis = screenTimeMillis,
                    todayUnlocks = unlocks
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun syncUsageStats() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSyncing = true, error = null)

                // Run the sync operation
                repository.syncUsageStats()

                // Reload usage data after sync
                val apps = getTodayUsage()
                val (screenTimeMillis, unlocks) = getTodaySummary()

                _uiState.value = _uiState.value.copy(
                    apps = apps,
                    isSyncing = false,
                    lastSyncTime = System.currentTimeMillis(),
                    todayScreenTimeMillis = screenTimeMillis,
                    todayUnlocks = unlocks
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    error = e.message ?: "Sync failed"
                )
            }
        }
    }

    private fun todayStartMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private suspend fun getTodaySummary(): Pair<Long, Int> {
        val startOfDay = todayStartMillis()
        val now = System.currentTimeMillis()
        val dailyPoints = repository.getUsageTotalsGroupedByDay(startOfDay, now + 1)
        val screenTimeMillis = dailyPoints.sumOf { it.totalUsageMillis }
        val unlocks = dailyPoints.sumOf { it.totalUnlockCount }
        return screenTimeMillis to unlocks
    }

    private suspend fun getTodayUsage(): List<AppUsage> {
        val startOfDay = todayStartMillis()
        val now = System.currentTimeMillis()

        val usageData = repository.getAppUsageSummaryInTimeRange(startOfDay, now)

        // Convert to the expected AppUsage format (keeping compatibility)
        return usageData.map { data ->
            AppUsage(
                packageName = data.packageName,
                appName = data.appName,
                icon = null, // Icon loading can be added later
                usageTimeMillis = data.usageDurationMillis
            )
        }
    }
}
