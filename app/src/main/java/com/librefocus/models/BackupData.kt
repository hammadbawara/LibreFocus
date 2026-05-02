package com.librefocus.models

import com.librefocus.data.local.database.entity.AchievementEntity
import com.librefocus.data.local.database.entity.AppCategoryEntity
import com.librefocus.data.local.database.entity.AppEntity
import com.librefocus.data.local.database.entity.DailyDeviceUsageEntity
import com.librefocus.data.local.database.entity.GoalHistoryEntity
import com.librefocus.data.local.database.entity.HourlyAppUsageEntity
import com.librefocus.data.local.database.entity.LaunchCountEntity
import com.librefocus.data.local.database.entity.LimitEntity
import com.librefocus.data.local.database.entity.PerfectDayEntity
import com.librefocus.data.local.database.entity.ScheduleLimitEntity
import com.librefocus.data.local.database.entity.SyncMetadataEntity
import com.librefocus.data.local.database.entity.UsageLimitEntity
import kotlinx.serialization.Serializable

/**
 * Root backup data structure.
 * Contains all database tables.
 */
@Serializable
data class BackupData(
    val version: Int = 2,
    val timestamp: Long,
    val database: DatabaseBackup
)

/**
 * Contains all Room database tables.
 */
@Serializable
data class DatabaseBackup(
    val appCategories: List<AppCategoryEntity>,
    val apps: List<AppEntity>,
    val hourlyAppUsage: List<HourlyAppUsageEntity>,
    val dailyDeviceUsage: List<DailyDeviceUsageEntity>,
    val limits: List<LimitEntity>,
    val scheduleLimits: List<ScheduleLimitEntity>,
    val usageLimits: List<UsageLimitEntity>,
    val launchCountLimits: List<LaunchCountEntity>,
    val syncMetadata: List<SyncMetadataEntity>,
    val perfectDays: List<PerfectDayEntity> = emptyList(),
    val achievements: List<AchievementEntity> = emptyList(),
    val goalHistory: List<GoalHistoryEntity> = emptyList()
)
