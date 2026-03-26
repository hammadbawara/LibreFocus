package com.librefocus.ui.limits

import com.librefocus.models.DayOfWeek
import com.librefocus.models.ResetPeriod
import com.librefocus.models.TimeSlot
import com.librefocus.models.UsageLimitType
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object LimitSummaryFormatter {

    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

    fun formatScheduleSummary(
        isAllDay: Boolean,
        timeSlots: List<TimeSlot>,
        selectedDays: Set<DayOfWeek>
    ): String {
        val timePart = if (isAllDay) {
            "All Day"
        } else {
            timeSlots.joinToString(", ") { slot ->
                "${formatMinutesToTime(slot.fromHour)} - ${formatMinutesToTime(slot.toHour)}"
            }
        }

        val daysPart = formatDays(selectedDays)

        return "$timePart, $daysPart"
    }

    fun formatUsageSummary(
        limitType: UsageLimitType,
        durationMinutes: Int,
        selectedDays: Set<DayOfWeek>
    ): String {
        val hours = durationMinutes / 60
        val minutes = durationMinutes % 60

        val durationPart = buildString {
            if (hours > 0) append("${hours}h ")
            if (minutes > 0) append("${minutes}m")
        }.trim()

        val typePart = when (limitType) {
            UsageLimitType.DAILY -> "day"
            UsageLimitType.HOURLY -> "hour"
        }

        val daysPart = formatDays(selectedDays)

        return "$durationPart / $typePart, $daysPart"
    }

    fun formatLaunchCountSummary(
        maxLaunches: Int,
        resetPeriod: ResetPeriod,
        selectedDays: Set<DayOfWeek>
    ): String {
        val resetPart = when (resetPeriod) {
            ResetPeriod.DAILY -> "day"
            ResetPeriod.WEEKLY -> "week"
        }

        val daysPart = formatDays(selectedDays)

        return "$maxLaunches launches / $resetPart, $daysPart"
    }

    private fun formatMinutesToTime(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        val time = LocalTime.of(hours, minutes)
        return time.format(timeFormatter)
    }

    fun formatDays(days: Set<DayOfWeek>): String {
        if (days.size == 7) return "All Week"
        if (days.isEmpty()) return "No days"

        val orderedDays = listOf(
            DayOfWeek.MON, DayOfWeek.TUE, DayOfWeek.WED,
            DayOfWeek.THU, DayOfWeek.FRI, DayOfWeek.SAT, DayOfWeek.SUN
        )

        return orderedDays
            .filter { it in days }
            .joinToString(" ") { formatDayAbbreviation(it) }
    }

    private fun formatDayAbbreviation(day: DayOfWeek): String {
        return when (day) {
            DayOfWeek.MON -> "Mon"
            DayOfWeek.TUE -> "Tue"
            DayOfWeek.WED -> "Wed"
            DayOfWeek.THU -> "Thu"
            DayOfWeek.FRI -> "Fri"
            DayOfWeek.SAT -> "Sat"
            DayOfWeek.SUN -> "Sun"
        }
    }

    fun formatDayLabel(day: DayOfWeek): String {
        return when (day) {
            DayOfWeek.MON -> "Monday"
            DayOfWeek.TUE -> "Tuesday"
            DayOfWeek.WED -> "Wednesday"
            DayOfWeek.THU -> "Thursday"
            DayOfWeek.FRI -> "Friday"
            DayOfWeek.SAT -> "Saturday"
            DayOfWeek.SUN -> "Sunday"
        }
    }
}
