package com.librefocus.models

/**
 * Domain model representing hourly aggregated usage data for an app.
 */
data class HourlyUsageData(
    val packageName: String,
    val appName: String,
    val hourStartUtc: Long,
    val usageDurationMillis: Long,
    val launchCount: Int
)
