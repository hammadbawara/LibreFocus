package com.librefocus.ui.stats

import com.librefocus.models.AppUsageData
import com.librefocus.models.UsageValuePoint

/**
 * UI model representing the stats screen data for a selected range.
 */
data class StatsUiState(
    val selectedRangeLabel: String = "Today",
    val totalUsageMillis: Long = 0L,
    val totalUnlocks: Int = 0,
    val averageSessionMillis: Long = 0L,
    val usagePoints: List<UsageValuePoint> = emptyList(),
    val appUsage: List<AppUsageData> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    // Display values for total and average (pre-calculated in ViewModel)
    val totalDisplayValue: String = "0m",
    val totalDisplayLabel: String = "Total",
    val averageDisplayValue: String = "0m",
    val averageDisplayLabel: String = "Avg per hour",
    val phaseOneInsights: PhaseOneInsights? = null,
    val phaseTwoInsights: PhaseTwoInsights? = null
)

data class PhaseOneInsights(
    val comparison: ComparisonInsight,
    val peakHours: PeakHoursInsight,
    val unlockEfficiency: UnlockEfficiencyInsight,
    val concentration: ConcentrationInsight
)

data class ComparisonInsight(
    val screenTime: MetricComparison,
    val opens: MetricComparison,
    val unlocks: MetricComparison,
    val mostUsedDayUtc: Long?
)

data class MetricComparison(
    val currentValue: Long,
    val previousValue: Long?,
    val deltaValue: Long?,
    val deltaPercent: Double?
)

data class PeakHoursInsight(
    val topHours: List<Int>,
    val lateNightPercentage: Int
)

data class UnlockEfficiencyInsight(
    val minutesPerUnlock: Double?,
    val minutesPerLaunch: Double?,
    val checkingHeavy: Boolean
)

data class ConcentrationInsight(
    val top1Percentage: Int,
    val top3Percentage: Int,
    val top5Percentage: Int
)

data class PhaseTwoInsights(
    val heatmap: UsageHeatmapInsight,
    val streaks: StreaksConsistencyInsight,
    val appSprawl: AppSprawlInsight
)

data class UsageHeatmapInsight(
    val cells: List<HeatmapCell>,
    val peakWeekday: Int?,
    val peakHour: Int?
)

data class HeatmapCell(
    val weekday: Int, // 1 = Monday, 7 = Sunday
    val hour: Int,
    val usageMinutes: Double,
    val intensity: Float
)

data class StreaksConsistencyInsight(
    val controlledDaysStreak: Int,
    val consistencyScore: Int,
    val volatilityMinutes: Int,
    val baselineMinutes: Int
)

data class AppSprawlInsight(
    val avgDistinctAppsPerDay: Double,
    val previousAvgDistinctAppsPerDay: Double?,
    val deltaPercent: Int?
)

/**
 * Represents the time period for statistics display.
 * Times are stored in UTC for data querying but are presented to users in their local timezone.
 */
data class StatsPeriodState(
    val startUtc: Long,  // UTC timestamp for period start
    val endUtc: Long,    // UTC timestamp for period end (exclusive)
    val label: String    // Formatted label in user's local time
)

enum class StatsMetric {
    ScreenTime,
    Opens
}

enum class StatsRange {
    Day,
    Week,
    Month,
    Custom
}
