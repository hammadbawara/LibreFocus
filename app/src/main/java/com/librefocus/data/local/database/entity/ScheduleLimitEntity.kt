package com.librefocus.data.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Entity for schedule-based limits. Contains time slots and days configuration.
 */
@Serializable
@Entity(
    tableName = "schedule_limits",
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
data class ScheduleLimitEntity(
    @PrimaryKey
    val limitId: String,
    
    val isAllDay: Boolean,
    
    /**
     * JSON array of TimeSlot objects. Converted by RoomTypeConverters.
     */
    val timeSlots: String,
    
    /**
     * Comma-separated day codes (MON,TUE,WED...). Converted by RoomTypeConverters.
     */
    val selectedDays: String
)
