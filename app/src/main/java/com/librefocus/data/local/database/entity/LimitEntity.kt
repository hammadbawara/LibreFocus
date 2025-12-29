package com.librefocus.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "limits")
data class LimitEntity(
    @PrimaryKey
    val id: String,
    
    val name: String,
    
    val isEnabled: Boolean,
    
    /**
     * Type discriminator: "SCHEDULE", "USAGE_LIMIT", or "LAUNCH_COUNT".
     */
    val limitType: String,

    /**
     * JSON array of app package names. Converted by RoomTypeConverters.
     */
    val selectedAppPackages: String,
    
    val createdAt: Long,
    
    val updatedAt: Long
)
