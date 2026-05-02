package com.librefocus.di

import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.librefocus.data.local.AppInfoProvider
import com.librefocus.data.local.UsageStatsProvider
import com.librefocus.data.local.database.UsageDatabase
import com.librefocus.data.repository.AppRepository
import com.librefocus.data.repository.BackupRestoreRepository
import com.librefocus.data.repository.CategoryRepository
import com.librefocus.data.repository.GamificationRepository
import com.librefocus.data.repository.LimitRepository
import com.librefocus.data.repository.UsageTrackingRepository
import org.koin.dsl.module

/**
 * Koin module for database and usage tracking dependencies.
 */
val databaseModule = module {

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `perfect_days` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `dateUtc` INTEGER NOT NULL,
                    `totalScreenTimeMillis` INTEGER NOT NULL,
                    `goalMinutes` INTEGER NOT NULL,
                    `calculatedAtUtc` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_perfect_days_dateUtc` ON `perfect_days` (`dateUtc`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_perfect_days_goalMinutes` ON `perfect_days` (`goalMinutes`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `achievements` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `type` TEXT NOT NULL,
                    `achievedAtUtc` INTEGER NOT NULL,
                    `sourceDateUtc` INTEGER NOT NULL,
                    `occurrenceCount` INTEGER NOT NULL,
                    `thresholdValue` INTEGER
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_achievements_type` ON `achievements` (`type`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_achievements_achievedAtUtc` ON `achievements` (`achievedAtUtc`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_achievements_sourceDateUtc` ON `achievements` (`sourceDateUtc`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_achievements_type_sourceDateUtc` ON `achievements` (`type`, `sourceDateUtc`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `goal_history` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `goalMinutes` INTEGER NOT NULL,
                    `startDateUtc` INTEGER NOT NULL,
                    `endDateUtc` INTEGER,
                    `createdAtUtc` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_goal_history_startDateUtc` ON `goal_history` (`startDateUtc`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_goal_history_endDateUtc` ON `goal_history` (`endDateUtc`)")
        }
    }
    
    // Room Database
    single {
        Room.databaseBuilder(
            get(),
            UsageDatabase::class.java,
            UsageDatabase.DATABASE_NAME
        )
            .addMigrations(MIGRATION_4_5)
            .build()
    }
    
    // DAOs
    single { get<UsageDatabase>().appCategoryDao() }
    single { get<UsageDatabase>().appDao() }
    single { get<UsageDatabase>().hourlyAppUsageDao() }
    single { get<UsageDatabase>().syncMetadataDao() }
    single { get<UsageDatabase>().dailyDeviceUsageDao() }
    single { get<UsageDatabase>().limitDao() }
    single { get<UsageDatabase>().chatMessageDao() }
    single { get<UsageDatabase>().perfectDayDao() }
    single { get<UsageDatabase>().achievementDao() }
    single { get<UsageDatabase>().goalHistoryDao() }

    // UsageStatsManager
    single {
        get<Context>().getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }
    
    // Data Source
    single {
        UsageStatsProvider(
            context = get(),
            usageStatsManager = get()
        )
    }

    single {
        AppInfoProvider(
            context = get()
        )
    }
    
    // Repositories
    single {
        UsageTrackingRepository(
            usageStatsProvider = get(),
            appCategoryDao = get(),
            appDao = get(),
            hourlyAppUsageDao = get(),
            dailyDeviceUsageDao = get(),
            syncMetadataDao = get(),
            appInfoProvider = get()
        )
    }
    
    single {
        AppRepository(
            context = get(),
            appInfoProvider = get()
        )
    }
    
    single {
        CategoryRepository(
            appCategoryDao = get(),
            appDao = get()
        )
    }
    
    single {
        LimitRepository(
            limitDao = get()
        )
    }

    single {
        GamificationRepository(
            preferencesRepository = get(),
            hourlyAppUsageDao = get(),
            perfectDayDao = get(),
            achievementDao = get(),
            goalHistoryDao = get(),
            syncMetadataDao = get()
        )
    }
    
    single {
        BackupRestoreRepository(
            context = get(),
            database = get(),
            appCategoryDao = get(),
            appDao = get(),
            hourlyAppUsageDao = get(),
            dailyDeviceUsageDao = get(),
            limitDao = get(),
            syncMetadataDao = get(),
            perfectDayDao = get(),
            achievementDao = get(),
            goalHistoryDao = get()
        )
    }
}
