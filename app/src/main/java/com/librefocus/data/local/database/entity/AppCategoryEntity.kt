package com.librefocus.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents an app category (predefined or custom) in the database.
 * All timestamps are stored in UTC to avoid timezone issues.
 */
@Entity(tableName = "app_categories")
data class AppCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    val categoryName: String,
    
    val isCustom: Boolean,
    
    /**
     * Timestamp when this category was added, stored as UTC epoch milliseconds
     */
    val addedAtUtc: Long
)
