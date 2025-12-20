package com.librefocus.ui.stats

import com.librefocus.models.UsageValuePoint
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
 * Formats the bottom label for chart based on the usage point and time format preference.
 * @param point UsageValuePoint with local time timestamp
 * @param range The stats range type
 * @param timeFormat User's time format preference ("12H" or "24H")
 */
fun formatBottomLabel(point: UsageValuePoint, range: StatsRange, timeFormat: String): String {
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

private val dayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd MMM").withLocale(Locale.getDefault())
