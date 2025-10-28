package com.librefocus.models

/**
 * Represents average app usage metrics for a given period.
 */
data class AppUsageAverages(
    val averageUsageMillis: Long,
    val averageLaunchCount: Int
)
