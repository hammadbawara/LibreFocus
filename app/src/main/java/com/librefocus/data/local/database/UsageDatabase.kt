package com.librefocus.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.librefocus.data.local.database.dao.AppCategoryDao
import com.librefocus.data.local.database.dao.AppDao
import com.librefocus.data.local.database.dao.HourlyAppUsageDao
import com.librefocus.data.local.database.dao.SyncMetadataDao
import com.librefocus.data.local.database.entity.AppCategoryEntity
import com.librefocus.data.local.database.entity.AppEntity
import com.librefocus.data.local.database.entity.HourlyAppUsageEntity
import com.librefocus.data.local.database.entity.SyncMetadataEntity

/**
 * Main Room database for LibreFocus app.
 * Contains all usage tracking, categories, and metadata.
 */
@Database(
    entities = [
        AppCategoryEntity::class,
        AppEntity::class,
        HourlyAppUsageEntity::class,
        SyncMetadataEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class UsageDatabase : RoomDatabase() {
    
    abstract fun appCategoryDao(): AppCategoryDao
    
    abstract fun appDao(): AppDao
    
    abstract fun hourlyAppUsageDao(): HourlyAppUsageDao
    
    abstract fun syncMetadataDao(): SyncMetadataDao
    
    companion object {
        const val DATABASE_NAME = "librefocus_usage_database"
    }
}
