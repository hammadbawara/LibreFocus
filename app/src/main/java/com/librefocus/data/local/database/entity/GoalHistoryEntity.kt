package com.librefocus.data.local.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "goal_history",
    indices = [
        Index(value = ["startDateUtc"], unique = true),
        Index(value = ["endDateUtc"])
    ]
)
data class GoalHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val goalMinutes: Int,

    val startDateUtc: Long,

    val endDateUtc: Long? = null,

    val createdAtUtc: Long
)