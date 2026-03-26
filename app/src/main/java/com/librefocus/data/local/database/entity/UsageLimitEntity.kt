package com.librefocus.data.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Entity for usage-based limits (daily or hourly duration restrictions).
 */
@Serializable
@Entity(
    tableName = "usage_limits",
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
data class UsageLimitEntity(
    @PrimaryKey
    val limitId: String,
    
    /**
     * "DAILY" or "HOURLY"
     */
    val limitType: String,
    
    val durationMinutes: Int,
    
    /**
     * Comma-separated day codes (MON,TUE,WED...). Converted by RoomTypeConverters.
     */
    val selectedDays: String
)
