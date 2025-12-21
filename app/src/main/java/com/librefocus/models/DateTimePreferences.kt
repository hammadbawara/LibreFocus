package com.librefocus.models

/**
 * Represents user preferences for date and time formatting.
 * All preferences are immutable and stored as Flow<DateTimePreferences> in the repository.
 */
data class DateTimePreferences(
    /**
     * Whether to use system defaults for all date/time settings.
     * When true, individual preferences below are ignored.
     */
    val useSystemDefaults: Boolean = true,
    
    /**
     * Time format preference.
     * Options: SYSTEM, TWELVE_HOUR, TWENTY_FOUR_HOUR
     */
    val timeFormat: TimeFormat = TimeFormat.SYSTEM,
    
    /**
     * Date format preference.
     * Options: SYSTEM, DD_MMM_YYYY, MM_DD_YYYY, YYYY_MM_DD
     */
    val dateFormat: DateFormat = DateFormat.SYSTEM,
    
    /**
     * Time zone ID (e.g., "America/New_York", "UTC", "Asia/Tokyo").
     * When null or "SYSTEM", uses the system default time zone.
     */
    val timeZoneId: String? = null
) {
    /**
     * Returns the effective time zone ID, falling back to system default if needed.
     */
    fun getEffectiveTimeZoneId(): String {
        return if (useSystemDefaults || timeZoneId.isNullOrEmpty() || timeZoneId == "SYSTEM") {
            java.time.ZoneId.systemDefault().id
        } else {
            timeZoneId
        }
    }
    
    /**
     * Returns the effective time format, respecting the useSystemDefaults flag.
     */
    fun getEffectiveTimeFormat(): TimeFormat {
        return if (useSystemDefaults) TimeFormat.SYSTEM else timeFormat
    }
    
    /**
     * Returns the effective date format, respecting the useSystemDefaults flag.
     */
    fun getEffectiveDateFormat(): DateFormat {
        return if (useSystemDefaults) DateFormat.SYSTEM else dateFormat
    }
}

/**
 * Time format options for displaying time values.
 */
enum class TimeFormat(val value: String) {
    SYSTEM("SYSTEM"),           // Use system/locale default
    TWELVE_HOUR("12H"),         // 12-hour format (e.g., 2:30 PM)
    TWENTY_FOUR_HOUR("24H");    // 24-hour format (e.g., 14:30)
    
    companion object {
        fun fromValue(value: String): TimeFormat {
            return entries.find { it.value == value } ?: SYSTEM
        }
    }
}

/**
 * Date format options for displaying date values.
 */
enum class DateFormat(val value: String, val pattern: String) {
    SYSTEM("SYSTEM", ""),                      // Use system/locale default
    DD_MMM_YYYY("DD_MMM_YYYY", "dd MMM yyyy"), // e.g., 21 Dec 2025
    MM_DD_YYYY("MM_DD_YYYY", "MM/dd/yyyy"),    // e.g., 12/21/2025
    YYYY_MM_DD("YYYY_MM_DD", "yyyy-MM-dd");    // e.g., 2025-12-21
    
    companion object {
        fun fromValue(value: String): DateFormat {
            return entries.find { it.value == value } ?: SYSTEM
        }
    }
}
