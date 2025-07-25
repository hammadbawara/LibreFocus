package com.librefocus.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.librefocus.data.repository.UsageTrackingRepository
import com.librefocus.models.AppUsage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class AnalyticsInsights(
    val totalUsageMillis: Long = 0,
    val peakHour: Int? = null,
    val topAppName: String? = null
)

data class HeatmapData(
    val dailyHourlyUsage: Map<Int, Map<Int, Long>> = emptyMap(),
    val maxUsagePerHour: Long = 0
)

data class HomeUiState(
    val apps: List<AppUsage> = emptyList(),
    val heatmapData: HeatmapData = HeatmapData(),
    val insights: AnalyticsInsights = AnalyticsInsights(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val error: String? = null,
    val lastSyncTime: Long? = null
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
                val (heatmap, insights) = getWeeklyAnalytics()
                _uiState.value = _uiState.value.copy(
                    apps = apps,
                    heatmapData = heatmap,
                    insights = insights,
                    isLoading = false
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
                val (heatmap, insights) = getWeeklyAnalytics()

                _uiState.value = _uiState.value.copy(
                    apps = apps,
                    heatmapData = heatmap,
                    insights = insights,
                    isSyncing = false,
                    lastSyncTime = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    error = e.message ?: "Sync failed"
                )
            }
        }
    }

    private suspend fun getTodayUsage(): List<AppUsage> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
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

    private suspend fun getWeeklyAnalytics(): Pair<HeatmapData, AnalyticsInsights> {
        // Calculate Mon-Sun bounds for the current week
        val calendar = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfWeek = calendar.timeInMillis
        val endOfWeek = startOfWeek + 7L * 24 * 60 * 60 * 1000

        val rawUsage = repository.getRawUsageInTimeRange(startOfWeek, endOfWeek)
        val appSummary = repository.getAppUsageSummaryInTimeRange(startOfWeek, endOfWeek)

        var totalUsage = 0L
        val hourlyTotalMap = mutableMapOf<Int, Long>()
        
        // Heatmap: DayOfWeek (1..7, 1=Mon) -> Hour (0..23) -> Duration
        val heatmapMap = mutableMapOf<Int, MutableMap<Int, Long>>()
        for (i in 1..7) {
            heatmapMap[i] = mutableMapOf()
        }

        var maxUsagePerHour = 0L

        rawUsage.forEach { usage ->
            totalUsage += usage.usageDurationMillis
            
            val cal = Calendar.getInstance().apply { timeInMillis = usage.hourStartUtc }
            val javaDay = cal.get(Calendar.DAY_OF_WEEK)
            val dayOfWeek = if (javaDay == Calendar.SUNDAY) 7 else javaDay - 1
            val hourOfDay = cal.get(Calendar.HOUR_OF_DAY)
            
            val currentMap = heatmapMap[dayOfWeek]!!
            val newUsage = (currentMap[hourOfDay] ?: 0L) + usage.usageDurationMillis
            currentMap[hourOfDay] = newUsage
            
            if (newUsage > maxUsagePerHour) {
                maxUsagePerHour = newUsage
            }
            
            hourlyTotalMap[hourOfDay] = (hourlyTotalMap[hourOfDay] ?: 0L) + usage.usageDurationMillis
        }

        val peakHour = hourlyTotalMap.maxByOrNull { it.value }?.key
        val topAppName = appSummary.firstOrNull()?.appName

        return Pair(
            HeatmapData(
                dailyHourlyUsage = heatmapMap,
                maxUsagePerHour = maxUsagePerHour
            ),
            AnalyticsInsights(
                totalUsageMillis = totalUsage,
                peakHour = peakHour,
                topAppName = topAppName
            )
        )
    }
}
