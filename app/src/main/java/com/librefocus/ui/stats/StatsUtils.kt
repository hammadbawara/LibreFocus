package com.librefocus.ui.stats

import com.librefocus.models.UsageValuePoint
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
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

fun formatBottomLabel(point: UsageValuePoint, range: StatsRange): String {
    val instant = Instant.ofEpochMilli(point.bucketStartUtc)
    val zonedDateTime = instant.atZone(ZoneOffset.UTC)

    return when (range) {
        StatsRange.Day -> hourFormatter.format(zonedDateTime)
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

private val hourFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH").withLocale(Locale.getDefault())

private val dayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd MMM").withLocale(Locale.getDefault())
