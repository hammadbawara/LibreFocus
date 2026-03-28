package com.librefocus.di

import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.room.Room
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
    
    // Room Database
    single {
        Room.databaseBuilder(
            get(),
            UsageDatabase::class.java,
            UsageDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration(true)
            .build()
    }
    
    // DAOs
    single { get<UsageDatabase>().appCategoryDao() }
    single { get<UsageDatabase>().appDao() }
    single { get<UsageDatabase>().hourlyAppUsageDao() }
    single { get<UsageDatabase>().syncMetadataDao() }
    single { get<UsageDatabase>().dailyDeviceUsageDao() }
    single { get<UsageDatabase>().limitDao() }
    single { get<UsageDatabase>().gamificationStatsDao() }
    single { get<UsageDatabase>().badgeDao() }
    single { get<UsageDatabase>().challengeDao() }
    single { get<UsageDatabase>().leaderboardEntryDao() }
    single { get<UsageDatabase>().streakDao() }
    
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
        BackupRestoreRepository(
            context = get(),
            database = get(),
            appCategoryDao = get(),
            appDao = get(),
            hourlyAppUsageDao = get(),
            dailyDeviceUsageDao = get(),
            limitDao = get(),
            syncMetadataDao = get()
        )
    }

    single {
        GamificationRepository(
            gamificationStatsDao = get(),
            badgeDao = get(),
            challengeDao = get(),
            leaderboardEntryDao = get(),
            streakDao = get(),
            hourlyAppUsageDao = get()
        )
    }
}
