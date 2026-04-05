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

data class TrendComparison(
    val percentageChange: Int = 0,
    val isIncrease: Boolean = false,
    val actionableAdvice: String = ""
)

data class DailyTrendData(
    val dayOfWeek: String,
    val timestampUtc: Long,
    val screenTimeMillis: Long,
    val unlocks: Int
)

data class AppUsageTrendData(
    val appName: String,
    val packageName: String,
    val usageMillis: Long
)

data class HomeUiState(
    val apps: List<AppUsage> = emptyList(),
    val heatmapData: HeatmapData = HeatmapData(),
    val insights: AnalyticsInsights = AnalyticsInsights(),
    val dailyTrends: List<DailyTrendData> = emptyList(),
    val topAppsUsage: List<AppUsageTrendData> = emptyList(),
    val screenTimeComparison: TrendComparison? = null,
    val unlocksComparison: TrendComparison? = null,
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
                loadAllData()
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
                loadAllData()

                _uiState.value = _uiState.value.copy(
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

    private suspend fun loadAllData() {
        val apps = getTodayUsage()
        val (heatmap, insights) = getWeeklyAnalytics()
        
        // Compute trends and comparisons
        val (currentWeekStart, currentWeekEnd) = getWeekBounds(0)
        val (previousWeekStart, previousWeekEnd) = getWeekBounds(-1)

        val dailyTrends = getDailyTrends(currentWeekStart, currentWeekEnd)
        val topAppsUsage = getTopAppsUsage(currentWeekStart, currentWeekEnd)

        val screenTimeComparison = getScreenTimeComparison(
            currentWeekStart, currentWeekEnd,
            previousWeekStart, previousWeekEnd,
            topAppsUsage.firstOrNull()?.appName
        )

        val unlocksComparison = getUnlocksComparison(
            currentWeekStart, currentWeekEnd,
            previousWeekStart, previousWeekEnd
        )

        _uiState.value = _uiState.value.copy(
            apps = apps,
            heatmapData = heatmap,
            insights = insights,
            dailyTrends = dailyTrends,
            topAppsUsage = topAppsUsage,
            screenTimeComparison = screenTimeComparison,
            unlocksComparison = unlocksComparison,
            isLoading = false
        )
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

    private fun getWeekBounds(weeksOffset: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.WEEK_OF_YEAR, weeksOffset)
        }
        val startOfWeek = calendar.timeInMillis
        val endOfWeek = startOfWeek + 7L * 24 * 60 * 60 * 1000
        return Pair(startOfWeek, endOfWeek)
    }

    private suspend fun getWeeklyAnalytics(): Pair<HeatmapData, AnalyticsInsights> {
        val (startOfWeek, endOfWeek) = getWeekBounds(0)

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

    private suspend fun getDailyTrends(startOfWeek: Long, endOfWeek: Long): List<DailyTrendData> {
        val dailyUsages = repository.getUsageTotalsGroupedByDay(startOfWeek, endOfWeek)
        val dailyUnlocks = repository.getDailyUnlockSummary(startOfWeek, endOfWeek)

        val unlockMap = dailyUnlocks.associateBy { it.dateUtc }
        val screenTimeMap = dailyUsages.associateBy { it.bucketStartUtc }

        val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val trends = mutableListOf<DailyTrendData>()

        for (i in 0..6) {
            val dayStart = startOfWeek + i * 24L * 60 * 60 * 1000
            val usageMillis = screenTimeMap[dayStart]?.totalUsageMillis ?: 0L
            val unlocks = unlockMap[dayStart]?.totalUnlocks ?: 0
            trends.add(
                DailyTrendData(
                    dayOfWeek = labels[i],
                    timestampUtc = dayStart,
                    screenTimeMillis = usageMillis,
                    unlocks = unlocks
                )
            )
        }
        return trends
    }

    private suspend fun getTopAppsUsage(startOfWeek: Long, endOfWeek: Long): List<AppUsageTrendData> {
        val summary = repository.getAppUsageSummaryInTimeRange(startOfWeek, endOfWeek)
        return summary.take(5).map {
            AppUsageTrendData(
                appName = it.appName,
                packageName = it.packageName,
                usageMillis = it.usageDurationMillis
            )
        }
    }

    private suspend fun getScreenTimeComparison(
        curStart: Long, curEnd: Long,
        prevStart: Long, prevEnd: Long,
        topAppName: String?
    ): TrendComparison {
        val currentUsages = repository.getUsageTotalsGroupedByDay(curStart, curEnd)
        val previousUsages = repository.getUsageTotalsGroupedByDay(prevStart, prevEnd)

        val currentTotal = currentUsages.sumOf { it.totalUsageMillis }
        val previousTotal = previousUsages.sumOf { it.totalUsageMillis }

        if (previousTotal == 0L) {
            return TrendComparison(0, false, "Not enough data from last week to compare screen time.")
        }

        val diff = currentTotal - previousTotal
        val percentage = ((diff.toDouble() / previousTotal.toDouble()) * 100).toInt()
        val isIncrease = diff > 0

        val advice = if (isIncrease) {
            "Your screen time is up by $percentage% compared to last week." + 
                if (topAppName != null) " Consider setting a daily usage limit on $topAppName." else ""
        } else {
            "Great job! You cut down your screen time by ${kotlin.math.abs(percentage)}% compared to last week."
        }

        return TrendComparison(
            percentageChange = kotlin.math.abs(percentage),
            isIncrease = isIncrease,
            actionableAdvice = advice
        )
    }

    private suspend fun getUnlocksComparison(
        curStart: Long, curEnd: Long,
        prevStart: Long, prevEnd: Long
    ): TrendComparison {
        val currentUnlocks = repository.getDailyUnlockSummary(curStart, curEnd)
        val previousUnlocks = repository.getDailyUnlockSummary(prevStart, prevEnd)

        val currentTotal = currentUnlocks.sumOf { it.totalUnlocks }
        val previousTotal = previousUnlocks.sumOf { it.totalUnlocks }

        if (previousTotal == 0) {
            return TrendComparison(0, false, "Not enough data from last week to compare unlocks.")
        }

        val diff = currentTotal - previousTotal
        val percentage = ((diff.toDouble() / previousTotal.toDouble()) * 100).toInt()
        val isIncrease = diff > 0

        val advice = if (isIncrease) {
            "You unlocked your phone $currentTotal times this week ($percentage% more). Try silencing non-essential notifications."
        } else {
            "You are checking your phone less often. Down by ${kotlin.math.abs(percentage)}%!"
        }

        return TrendComparison(
            percentageChange = kotlin.math.abs(percentage),
            isIncrease = isIncrease,
            actionableAdvice = advice
        )
    }
}
