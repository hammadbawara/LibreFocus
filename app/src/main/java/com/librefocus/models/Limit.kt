package com.librefocus.models

/**
 * Domain model representing an app usage limit.
 * Supports three types: Schedule-based, Usage-based, and Launch count-based limits.
 */
sealed class Limit {
    abstract val id: String
    abstract val name: String
    abstract val isEnabled: Boolean
    abstract val selectedAppPackages: List<String>
    abstract val createdAt: Long
    abstract val updatedAt: Long
    
    /**
     * Schedule-based limit that blocks apps during specific time slots on selected days.
     */
    data class Schedule(
        override val id: String,
        override val name: String,
        override val isEnabled: Boolean,
        override val selectedAppPackages: List<String>,
        override val createdAt: Long,
        override val updatedAt: Long,
        val isAllDay: Boolean,
        val timeSlots: List<TimeSlot>,
        val selectedDays: Set<DayOfWeek>
    ) : Limit()
    
    /**
     * Usage-based limit that restricts app usage to a specific duration (daily or hourly).
     */
    data class UsageLimit(
        override val id: String,
        override val name: String,
        override val isEnabled: Boolean,
        override val selectedAppPackages: List<String>,
        override val createdAt: Long,
        override val updatedAt: Long,
        val limitType: UsageLimitType,
        val durationMinutes: Int,
        val selectedDays: Set<DayOfWeek>
    ) : Limit()
    
    /**
     * Launch count limit that restricts the number of times apps can be opened.
     */
    data class LaunchCount(
        override val id: String,
        override val name: String,
        override val isEnabled: Boolean,
        override val selectedAppPackages: List<String>,
        override val createdAt: Long,
        override val updatedAt: Long,
        val maxLaunches: Int,
        val resetPeriod: ResetPeriod,
        val selectedDays: Set<DayOfWeek>
    ) : Limit()
}

/**
 * Represents a time slot with start and end hours (24-hour format).
 */
data class TimeSlot(
    val fromHour: Int,
    val toHour: Int
)

enum class DayOfWeek {
    MON, TUE, WED, THU, FRI, SAT, SUN
}

enum class UsageLimitType {
    DAILY, HOURLY
}

enum class ResetPeriod {
    DAILY, WEEKLY
}
