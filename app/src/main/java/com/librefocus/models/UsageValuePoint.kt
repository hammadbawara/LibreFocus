package com.librefocus.models

/**
 * Represents an aggregated usage value for a specific time bucket.
 */
data class UsageValuePoint(
    val bucketStartUtc: Long,
    val totalUsageMillis: Long,
    val totalLaunchCount: Int
)
