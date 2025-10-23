package com.librefocus.models

/**
 * Domain model representing a single usage event from the system.
 */
data class UsageEventData(
    val packageName: String,
    val timestampUtc: Long,
    val eventType: Int
)
