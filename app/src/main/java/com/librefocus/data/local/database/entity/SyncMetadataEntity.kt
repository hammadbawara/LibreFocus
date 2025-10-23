package com.librefocus.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores metadata about the last sync operation.
 * Used to track the last time usage stats were fetched from the system.
 */
@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey
    val key: String,
    
    /**
     * Value stored as UTC epoch milliseconds
     */
    val valueUtc: Long
)
