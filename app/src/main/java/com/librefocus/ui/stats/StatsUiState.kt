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
    val errorMessage: String? = null
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
