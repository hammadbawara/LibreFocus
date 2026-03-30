package com.librefocus.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.librefocus.data.local.database.converter.RoomTypeConverters
import com.librefocus.data.local.database.dao.AppCategoryDao
import com.librefocus.data.local.database.dao.AppDao
import com.librefocus.data.local.database.dao.DailyDeviceUsageDao
import com.librefocus.data.local.database.dao.HourlyAppUsageDao
import com.librefocus.data.local.database.dao.LimitDao
import com.librefocus.data.local.database.dao.SyncMetadataDao
import com.librefocus.data.local.database.dao.ChatMessageDao
import com.librefocus.data.local.database.entity.AppCategoryEntity
import com.librefocus.data.local.database.entity.AppEntity
import com.librefocus.data.local.database.entity.DailyDeviceUsageEntity
import com.librefocus.data.local.database.entity.HourlyAppUsageEntity
import com.librefocus.data.local.database.entity.LaunchCountEntity
import com.librefocus.data.local.database.entity.LimitEntity
import com.librefocus.data.local.database.entity.ScheduleLimitEntity
import com.librefocus.data.local.database.entity.SyncMetadataEntity
import com.librefocus.data.local.database.entity.UsageLimitEntity
import com.librefocus.data.local.database.entity.ChatMessageEntity

/**
 * Main Room database for LibreFocus app.
 * Contains all usage tracking, categories, limits, and metadata.
 */
@Database(
    entities = [
        AppCategoryEntity::class,
        AppEntity::class,
        HourlyAppUsageEntity::class,
        DailyDeviceUsageEntity::class,
        SyncMetadataEntity::class,
        LimitEntity::class,
        ScheduleLimitEntity::class,
        UsageLimitEntity::class,
        LaunchCountEntity::class,
        ChatMessageEntity::class
    ],
    version = 4,
    // Schema export disabled because a schema export directory is not configured for KSP.
    // Set to true and configure `room.schemaLocation` in Gradle if you want exported schemas.
    exportSchema = false
)
@TypeConverters(RoomTypeConverters::class)
abstract class UsageDatabase : RoomDatabase() {
    
    abstract fun appCategoryDao(): AppCategoryDao
    
    abstract fun appDao(): AppDao
    
    abstract fun hourlyAppUsageDao(): HourlyAppUsageDao
    
    abstract fun syncMetadataDao(): SyncMetadataDao

    abstract fun dailyDeviceUsageDao(): DailyDeviceUsageDao
    
    abstract fun limitDao(): LimitDao

    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        const val DATABASE_NAME = "librefocus_usage_database"
    }
}
