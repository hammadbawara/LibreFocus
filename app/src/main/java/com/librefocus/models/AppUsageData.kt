package com.librefocus.models

/**
 * Domain model representing app usage data.
 * Used for transferring data between layers.
 */
data class AppUsageData(
    val packageName: String,
    val appName: String,
    val usageDurationMillis: Long,
    val launchCount: Int
)
