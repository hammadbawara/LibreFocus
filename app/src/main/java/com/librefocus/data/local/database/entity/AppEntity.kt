package com.librefocus.data.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents an installed application tracked by LibreFocus.
 * Each app belongs to a category.
 */
@Entity(
    tableName = "apps",
    foreignKeys = [
        ForeignKey(
            entity = AppCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["packageName"], unique = true),
        Index(value = ["categoryId"])
    ]
)
data class AppEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    val packageName: String,
    
    val appName: String,
    
    val categoryId: Int
)
