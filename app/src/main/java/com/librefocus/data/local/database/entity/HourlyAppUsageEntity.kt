package com.librefocus.data.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents hourly aggregated app usage data.
 * All timestamps are stored in UTC to avoid timezone issues.
 */
@Entity(
    tableName = "hourly_app_usage",
    foreignKeys = [
        ForeignKey(
            entity = AppEntity::class,
            parentColumns = ["id"],
            childColumns = ["appId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["appId"]),
        Index(value = ["hourStartUtc"]),
        Index(value = ["appId", "hourStartUtc"], unique = true)
    ]
)
data class HourlyAppUsageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    val appId: Int,
    
    /**
     * UTC timestamp of the start of the hour (rounded down to the hour)
     */
    val hourStartUtc: Long,
    
    /**
     * Total usage duration in milliseconds for this hour
     */
    val usageDurationMillis: Long,
    
    /**
     * Number of times the app was launched during this hour
     */
    val launchCount: Int
)
