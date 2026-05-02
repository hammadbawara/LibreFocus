package com.librefocus.data.local.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "achievements",
    indices = [
        Index(value = ["type"]),
        Index(value = ["achievedAtUtc"]),
        Index(value = ["sourceDateUtc"]),
        Index(value = ["type", "sourceDateUtc"], unique = true)
    ]
)
data class AchievementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val type: String,

    val achievedAtUtc: Long,

    val sourceDateUtc: Long,

    val occurrenceCount: Int,

    val thresholdValue: Int? = null
)