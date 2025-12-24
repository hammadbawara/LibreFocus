package com.librefocus.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_categories")
data class AppCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    val categoryName: String,
    
    val isCustom: Boolean,
    
    val systemCategoryId: Int? = null,
    
    val addedAtUtc: Long
)
