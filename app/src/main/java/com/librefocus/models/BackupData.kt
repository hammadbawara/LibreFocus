package com.librefocus.models

import com.librefocus.data.local.database.entity.AppCategoryEntity
import com.librefocus.data.local.database.entity.AppEntity
import com.librefocus.data.local.database.entity.HourlyAppUsageEntity
import com.librefocus.data.local.database.entity.SyncMetadataEntity
import kotlinx.serialization.Serializable

/**
 * Root backup data structure.
 * Contains essential app usage data only.
 */
@Serializable
data class BackupData(
    val version: Int = 1,
    val timestamp: Long,
    val database: DatabaseBackup
)

/**
 * Contains essential Room database tables for usage tracking.
 */
@Serializable
data class DatabaseBackup(
    val appCategories: List<AppCategoryEntity>,
    val apps: List<AppEntity>,
    val hourlyAppUsage: List<HourlyAppUsageEntity>,
    val syncMetadata: List<SyncMetadataEntity>
)
