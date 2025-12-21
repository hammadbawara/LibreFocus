package com.librefocus.ui.stats

import com.librefocus.models.UsageValuePoint
import com.librefocus.utils.FormattedDateTimePreferences
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

fun StatsRange.displayName(): String {
    return when (this) {
        StatsRange.Day -> "Day"
        StatsRange.Week -> "Week"
        StatsRange.Month -> "Month"
        StatsRange.Custom -> "Custom"
    }
}

fun StatsMetric.displayName(): String {
    return when (this) {
        StatsMetric.ScreenTime -> "Screen time"
        StatsMetric.Opens -> "Opens"
    }
}

fun formatDuration(millis: Long): String {
    if (millis <= 0L) return "0m"
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    return buildString {
        if (hours > 0) {
            append(hours)
            append('h')
            if (minutes > 0) append(' ')
        }
        if (minutes > 0 || hours == 0L) {
            append(minutes)
            append('m')
        }
    }
}

/**
 * Formats the bottom label for chart based on the usage point and formatted preferences.
 * @param point UsageValuePoint with local time timestamp
 * @param range The stats range type
 * @param formatted Formatted date/time preferences
 */
fun formatBottomLabel(point: UsageValuePoint, range: StatsRange, formatted: FormattedDateTimePreferences?): String {
    if (formatted == null) {
        // Fallback to default formatting if preferences not yet loaded
        return formatBottomLabelLegacy(point, range, "24H")
    }
    
    return when (range) {
        StatsRange.Day -> formatted.formatHour(point.bucketStartUtc)
        StatsRange.Week, StatsRange.Month, StatsRange.Custom -> formatted.formatShortDate(point.bucketStartUtc)
    }
}

/**
 * Legacy bottom label formatter for backward compatibility.
 * @param point UsageValuePoint with local time timestamp
 * @param range The stats range type
 * @param timeFormat User's time format preference ("12H" or "24H")
 */
@Deprecated("Use formatBottomLabel with FormattedDateTimePreferences instead")
fun formatBottomLabelLegacy(point: UsageValuePoint, range: StatsRange, timeFormat: String): String {
    val instant = Instant.ofEpochMilli(point.bucketStartUtc)
    val zonedDateTime = instant.atZone(ZoneId.systemDefault())

    return when (range) {
        StatsRange.Day -> {
            // Use 12H or 24H format based on user preference
            val formatter = if (timeFormat == "12H") {
                DateTimeFormatter.ofPattern("ha").withLocale(Locale.getDefault())
            } else {
                DateTimeFormatter.ofPattern("HH").withLocale(Locale.getDefault())
            }
            formatter.format(zonedDateTime)
        }
        StatsRange.Week, StatsRange.Month, StatsRange.Custom -> dayFormatter.format(zonedDateTime)
    }
}

fun formatMinutesLabel(value: Double): String {
    val minutes = value.roundToInt()
    return if (minutes < 60) {
        "${minutes}m"
    } else {
        val hours = minutes / 60
        val remaining = minutes % 60
        if (remaining == 0) {
            "${hours}h"
        } else {
            "${hours}h ${remaining}m"
        }
    }
}

/**
 * Calculates the total value for the given metric from usage points.
 * @param usagePoints List of usage data points
 * @param metric The metric type (ScreenTime or Opens)
 * @return Total value in milliseconds (for ScreenTime) or count (for Opens)
 */
fun calculateTotal(usagePoints: List<UsageValuePoint>, metric: StatsMetric): Long {
    if (usagePoints.isEmpty()) return 0L
    return when (metric) {
        StatsMetric.ScreenTime -> usagePoints.sumOf { it.totalUsageMillis }
        StatsMetric.Opens -> usagePoints.sumOf { it.totalLaunchCount.toLong() }
    }
}

/**
 * Calculates the average value for the given metric from usage points.
 * @param usagePoints List of usage data points
 * @param metric The metric type (ScreenTime or Opens)
 * @param range The time range for proper averaging context
 * @return Average value in milliseconds (for ScreenTime) or count (for Opens)
 */
fun calculateAverage(usagePoints: List<UsageValuePoint>, metric: StatsMetric, range: StatsRange): Long {
    if (usagePoints.isEmpty()) return 0L
    val total = calculateTotal(usagePoints, metric)
    return total / usagePoints.size
}

/**
 * Formats the average label based on the range and metric.
 * @param range The time range
 * @param metric The metric type
 * @return Formatted label string (e.g., "Avg per hour", "Avg per day")
 */
fun formatAverageLabel(range: StatsRange, metric: StatsMetric): String {
    return when (range) {
        StatsRange.Day -> when (metric) {
            StatsMetric.ScreenTime -> "Avg per hour"
            StatsMetric.Opens -> "Avg opens per hour"
        }
        else -> when (metric) {
            StatsMetric.ScreenTime -> "Avg per day"
            StatsMetric.Opens -> "Avg opens per day"
        }
    }
}

/**
 * Formats the total label based on the metric.
 * @param metric The metric type
 * @return Formatted label string (e.g., "Total", "Total opens")
 */
fun formatTotalLabel(metric: StatsMetric): String {
    return when (metric) {
        StatsMetric.ScreenTime -> "Total"
        StatsMetric.Opens -> "Total opens"
    }
}

private val dayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd MMM").withLocale(Locale.getDefault())
