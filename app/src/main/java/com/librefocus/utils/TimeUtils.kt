package com.librefocus.utils

import java.util.concurrent.TimeUnit

private val HOUR_MILLIS = TimeUnit.HOURS.toMillis(1)
private val DAY_MILLIS = TimeUnit.DAYS.toMillis(1)

/**
 * Rounds a UTC timestamp down to the start of the hour.
 */
fun roundToHourStart(utcMillis: Long): Long {
    return utcMillis - (utcMillis % HOUR_MILLIS)
}

/**
 * Rounds a UTC timestamp down to the start of the day (midnight UTC).
 */
fun roundToDayStart(utcMillis: Long): Long {
    return utcMillis - (utcMillis % DAY_MILLIS)
}

/**
 * Calculates the hour of day (0-23) from a UTC timestamp.
 */
fun extractUtcHourOfDay(utcMillis: Long): Int {
    val millisIntoDay = utcMillis - roundToDayStart(utcMillis)
    return (millisIntoDay / HOUR_MILLIS).toInt()
}
