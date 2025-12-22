package com.librefocus.utils

import android.content.Context
import com.librefocus.models.DateFormat
import com.librefocus.models.DateTimePreferences
import com.librefocus.models.TimeFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import android.text.format.DateFormat as AndroidDateFormat

/**
 * Centralized manager for date and time formatting throughout the app.
 * 
 * This utility ensures:
 * - All timestamps are stored in UTC
 * - Conversion to user/system timezone happens only at display time
 * - User preferences are respected consistently across the app
 * - No hardcoded date/time patterns anywhere
 * 
 * Usage:
 * ```
 * dateTimeFormatter.formatDateTime(utcMillis)
 * dateTimeFormatter.formatDate(utcMillis)
 * dateTimeFormatter.formatTime(utcMillis)
 * ```
 */
class DateTimeFormatterManager(
    private val context: Context,
    preferencesFlow: Flow<DateTimePreferences>,
    private val locale: Locale = Locale.getDefault()
) {
    /**
     * Flow of formatted preferences with cached formatters.
     * Subscribe to this in ViewModels to react to preference changes.
     */
    val formattedPreferences: Flow<FormattedDateTimePreferences> = preferencesFlow.map { prefs ->
        FormattedDateTimePreferences(
            preferences = prefs,
            zoneId = ZoneId.of(prefs.getEffectiveTimeZoneId()),
            timeFormatter = createTimeFormatter(prefs),
            dateFormatter = createDateFormatter(prefs),
            dateTimeFormatter = createDateTimeFormatter(prefs),
            shortDateFormatter = createShortDateFormatter(prefs),
            dayLabelFormatter = createDayLabelFormatter(prefs),
            monthLabelFormatter = createMonthLabelFormatter(prefs),
            hourFormatter = createHourFormatter(prefs),
            hourRangeFormatter = createHourRangeFormatter(prefs)
        )
    }
    
    /**
     * Creates a time formatter based on user preferences.
     */
    private fun createTimeFormatter(prefs: DateTimePreferences): DateTimeFormatter {
        val timeFormat = prefs.getEffectiveTimeFormat()
        val zoneId = ZoneId.of(prefs.getEffectiveTimeZoneId())
        
        return when (timeFormat) {
            TimeFormat.SYSTEM -> {
                // Use system locale to determine 12h/24h format
                val pattern = if (AndroidDateFormat.is24HourFormat(context)) {
                    "HH:mm"
                } else {
                    "h:mm a"
                }
                DateTimeFormatter.ofPattern(pattern, locale).withZone(zoneId)
            }
            TimeFormat.TWELVE_HOUR -> {
                DateTimeFormatter.ofPattern("h:mm a", locale).withZone(zoneId)
            }
            TimeFormat.TWENTY_FOUR_HOUR -> {
                DateTimeFormatter.ofPattern("HH:mm", locale).withZone(zoneId)
            }
        }
    }
    
    /**
     * Creates a date formatter based on user preferences.
     */
    private fun createDateFormatter(prefs: DateTimePreferences): DateTimeFormatter {
        val dateFormat = prefs.getEffectiveDateFormat()
        val zoneId = ZoneId.of(prefs.getEffectiveTimeZoneId())
        
        return when (dateFormat) {
            DateFormat.SYSTEM -> {
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                    .withLocale(locale)
                    .withZone(zoneId)
            }
            else -> {
                DateTimeFormatter.ofPattern(dateFormat.pattern, locale).withZone(zoneId)
            }
        }
    }
    
    /**
     * Creates a combined date-time formatter.
     */
    private fun createDateTimeFormatter(prefs: DateTimePreferences): DateTimeFormatter {
        val timeFormat = prefs.getEffectiveTimeFormat()
        val dateFormat = prefs.getEffectiveDateFormat()
        val zoneId = ZoneId.of(prefs.getEffectiveTimeZoneId())
        
        val timePattern = when (timeFormat) {
            TimeFormat.SYSTEM -> if (AndroidDateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
            TimeFormat.TWELVE_HOUR -> "h:mm a"
            TimeFormat.TWENTY_FOUR_HOUR -> "HH:mm"
        }
        
        val datePattern = when (dateFormat) {
            DateFormat.SYSTEM -> return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .withLocale(locale)
                .withZone(zoneId)
            else -> dateFormat.pattern
        }
        
        val pattern = "$datePattern $timePattern"
        return DateTimeFormatter.ofPattern(pattern, locale).withZone(zoneId)
    }
    
    /**
     * Creates a short date formatter (e.g., "21 Dec").
     */
    private fun createShortDateFormatter(prefs: DateTimePreferences): DateTimeFormatter {
        val zoneId = ZoneId.of(prefs.getEffectiveTimeZoneId())
        return DateTimeFormatter.ofPattern("dd MMM", locale).withZone(zoneId)
    }
    
    /**
     * Creates a day label formatter (e.g., "Sat, 21 Dec").
     */
    private fun createDayLabelFormatter(prefs: DateTimePreferences): DateTimeFormatter {
        val zoneId = ZoneId.of(prefs.getEffectiveTimeZoneId())
        return DateTimeFormatter.ofPattern("EEE, dd MMM", locale).withZone(zoneId)
    }
    
    /**
     * Creates a month label formatter (e.g., "December 2025").
     */
    private fun createMonthLabelFormatter(prefs: DateTimePreferences): DateTimeFormatter {
        val zoneId = ZoneId.of(prefs.getEffectiveTimeZoneId())
        return DateTimeFormatter.ofPattern("MMMM yyyy", locale).withZone(zoneId)
    }
    
    /**
     * Creates an hour formatter for chart labels.
     */
    private fun createHourFormatter(prefs: DateTimePreferences): DateTimeFormatter {
        val timeFormat = prefs.getEffectiveTimeFormat()
        val zoneId = ZoneId.of(prefs.getEffectiveTimeZoneId())
        
        val pattern = when (timeFormat) {
            TimeFormat.SYSTEM -> if (AndroidDateFormat.is24HourFormat(context)) "HH" else "ha"
            TimeFormat.TWELVE_HOUR -> "ha"
            TimeFormat.TWENTY_FOUR_HOUR -> "HH"
        }
        
        return DateTimeFormatter.ofPattern(pattern, locale).withZone(zoneId)
    }
    
    /**
     * Creates an hour range formatter (e.g., "14:00 - 15:00" or "2 PM - 3 PM").
     */
    private fun createHourRangeFormatter(prefs: DateTimePreferences): DateTimeFormatter {
        val timeFormat = prefs.getEffectiveTimeFormat()
        val zoneId = ZoneId.of(prefs.getEffectiveTimeZoneId())
        
        val pattern = when (timeFormat) {
            TimeFormat.SYSTEM -> if (AndroidDateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
            TimeFormat.TWELVE_HOUR -> "h:mm a"
            TimeFormat.TWENTY_FOUR_HOUR -> "HH:mm"
        }
        
        return DateTimeFormatter.ofPattern(pattern, locale).withZone(zoneId)
    }
}

/**
 * Container for formatted preferences with cached formatters.
 * This is emitted by the DateTimeFormatterManager flow.
 */
data class FormattedDateTimePreferences(
    val preferences: DateTimePreferences,
    val zoneId: ZoneId,
    val timeFormatter: DateTimeFormatter,
    val dateFormatter: DateTimeFormatter,
    val dateTimeFormatter: DateTimeFormatter,
    val shortDateFormatter: DateTimeFormatter,
    val dayLabelFormatter: DateTimeFormatter,
    val monthLabelFormatter: DateTimeFormatter,
    val hourFormatter: DateTimeFormatter,
    val hourRangeFormatter: DateTimeFormatter
) {
    /**
     * Formats a UTC timestamp as time only (e.g., "14:30" or "2:30 PM").
     */
    fun formatTime(utcMillis: Long): String {
        return timeFormatter.format(Instant.ofEpochMilli(utcMillis))
    }
    
    /**
     * Formats a UTC timestamp as date only (e.g., "21 Dec 2025").
     */
    fun formatDate(utcMillis: Long): String {
        return dateFormatter.format(Instant.ofEpochMilli(utcMillis))
    }
    
    /**
     * Formats a UTC timestamp as date and time (e.g., "21 Dec 2025 14:30").
     */
    fun formatDateTime(utcMillis: Long): String {
        return dateTimeFormatter.format(Instant.ofEpochMilli(utcMillis))
    }
    
    /**
     * Formats a UTC timestamp as short date (e.g., "21 Dec").
     */
    fun formatShortDate(utcMillis: Long): String {
        return shortDateFormatter.format(Instant.ofEpochMilli(utcMillis))
    }
    
    /**
     * Formats a UTC timestamp as day label (e.g., "Sat, 21 Dec").
     */
    fun formatDayLabel(utcMillis: Long): String {
        return dayLabelFormatter.format(Instant.ofEpochMilli(utcMillis))
    }
    
    /**
     * Formats a UTC timestamp as month label (e.g., "December 2025").
     */
    fun formatMonthLabel(utcMillis: Long): String {
        return monthLabelFormatter.format(Instant.ofEpochMilli(utcMillis))
    }
    
    /**
     * Formats a UTC timestamp as hour (e.g., "14" or "2 PM").
     */
    fun formatHour(utcMillis: Long): String {
        return hourFormatter.format(Instant.ofEpochMilli(utcMillis))
    }
    
    /**
     * Formats a time range (e.g., "14:00 - 15:00").
     */
    fun formatHourRange(startUtcMillis: Long, endUtcMillis: Long): String {
        val start = hourRangeFormatter.format(Instant.ofEpochMilli(startUtcMillis))
        val end = hourRangeFormatter.format(Instant.ofEpochMilli(endUtcMillis))
        return "$start - $end"
    }
    
    /**
     * Formats a date range (e.g., "21 Dec - 27 Dec").
     */
    fun formatDateRange(startUtcMillis: Long, endUtcMillis: Long): String {
        val start = shortDateFormatter.format(Instant.ofEpochMilli(startUtcMillis))
        val end = shortDateFormatter.format(Instant.ofEpochMilli(endUtcMillis - 1))
        return "$start – $end"
    }
    
    /**
     * Converts UTC milliseconds to ZonedDateTime in the effective time zone.
     */
    fun toZonedDateTime(utcMillis: Long): ZonedDateTime {
        return Instant.ofEpochMilli(utcMillis).atZone(zoneId)
    }
    
    /**
     * Converts UTC milliseconds to LocalDate in the effective time zone.
     */
    fun toLocalDate(utcMillis: Long): LocalDate {
        return Instant.ofEpochMilli(utcMillis).atZone(zoneId).toLocalDate()
    }
    
    /**
     * Converts UTC milliseconds to LocalDateTime in the effective time zone.
     */
    fun toLocalDateTime(utcMillis: Long): LocalDateTime {
        return Instant.ofEpochMilli(utcMillis).atZone(zoneId).toLocalDateTime()
    }
}
