package com.librefocus.data.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for launch count-based limits (restrict number of app launches).
 */
@Entity(
    tableName = "launch_count_limits",
    foreignKeys = [
        ForeignKey(
            entity = LimitEntity::class,
            parentColumns = ["id"],
            childColumns = ["limitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["limitId"])]
)
data class LaunchCountEntity(
    @PrimaryKey
    val limitId: String,
    
    val maxLaunches: Int,
    
    /**
     * "DAILY" or "WEEKLY"
     */
    val resetPeriod: String,
    
    /**
     * Comma-separated day codes (MON,TUE,WED...). Converted by RoomTypeConverters.
     */
    val selectedDays: String
)
