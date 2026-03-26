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
    val phaseTwoInsights: PhaseTwoInsights? = null,
    val phaseThreeInsights: PhaseThreeInsights? = null
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
    val peakHour: Int?,
    val windowDays: Int
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

// ─────────────────────────────────────────────────────────────────────────────
// Phase 3: Predictions & Correlations
// ─────────────────────────────────────────────────────────────────────────────

enum class ForecastConfidence { LOW, MEDIUM, HIGH }

enum class CorrelationStrength { NONE, WEAK, MODERATE, STRONG }

enum class CorrelationMessage {
    MORE_UNLOCKS_MORE_TIME,
    UNLOCKS_NOT_PREDICTIVE,
    MORE_UNLOCKS_SHORTER_SESSIONS
}

/**
 * End-of-day forecast for today.
 * Only produced when the selected Day period is the current calendar day.
 *
 * @param actualUsageMillis usage recorded so far today.
 * @param forecastedTotalMillis projected total for the full day.
 * @param remainingForecastMillis projected usage still to come this day.
 * @param typicalSameWeekdayMillis median total for the same weekday over last 4 weeks (null if no history).
 * @param typicalDeltaPercent % delta between projected total and typical (null when no history).
 * @param currentHour hour-of-day at computation time.
 * @param weeksUsed number of past same-weekday weeks used in forecast (0–4).
 * @param confidence forecast confidence level derived from weeksUsed.
 * @param weekdayName localised weekday name for display (e.g. "Thursday").
 */
data class EndOfDayForecastInsight(
    val actualUsageMillis: Long,
    val forecastedTotalMillis: Long,
    val remainingForecastMillis: Long,
    val typicalSameWeekdayMillis: Long?,
    val typicalDeltaPercent: Int?,
    val currentHour: Int,
    val weeksUsed: Int,
    val confidence: ForecastConfidence,
    val weekdayName: String
)

/**
 * Pearson correlation between daily unlock count and daily screen time over the last 30 days.
 *
 * @param pearsonR raw correlation coefficient (−1.0 to 1.0).
 * @param strength categorised magnitude.
 * @param dayCount number of paired days used in the calculation.
 * @param message human-readable interpretation to display in the UI.
 */
data class CorrelationInsight(
    val pearsonR: Double,
    val strength: CorrelationStrength,
    val dayCount: Int,
    val message: CorrelationMessage
)

data class PhaseThreeInsights(
    val forecast: EndOfDayForecastInsight?,   // null when range != Day
    val correlation: CorrelationInsight?       // null when < 7 paired days
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
    Opens,
    Unlocks
}

enum class StatsRange {
    Day,
    Week,
    Month,
    Custom
}
