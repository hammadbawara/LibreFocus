package com.librefocus.data.local.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "perfect_days",
    indices = [
        Index(value = ["dateUtc"], unique = true),
        Index(value = ["goalMinutes"])
    ]
)
data class PerfectDayEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val dateUtc: Long,

    val totalScreenTimeMillis: Long,

    val goalMinutes: Int,

    val calculatedAtUtc: Long
)