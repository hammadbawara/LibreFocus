package com.librefocus.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents aggregated daily device unlock information.
 * Stores unlock counts per hour to enable fast analytics queries.
 */
@Entity(tableName = "daily_device_usage")
data class DailyDeviceUsageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /** UTC timestamp aligned to midnight for the represented day. */
    val dateUtc: Long,

    /** Map of hour (0-23) to unlock count for that hour. */
    val hourlyUnlocks: Map<Int, Int>,

    /** Total unlocks recorded for the day. */
    val totalUnlocks: Int,

    /** UTC timestamp of the last time this record was updated. */
    val lastUpdatedUtc: Long
)
