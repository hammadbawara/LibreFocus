package com.librefocus.ui.stats

import com.librefocus.models.AppUsageData
import com.librefocus.models.UsageValuePoint



/**
 * Represents the time period for statistics display.
 * Times are stored in UTC for data querying but are presented to users in their local timezone.
 */
data class StatsPeriodState(
    val startUtc: Long,  // UTC timestamp for period start
    val endUtc: Long,    // UTC timestamp for period end (exclusive)
    val label: String    // Formatted label in user's local time
)

