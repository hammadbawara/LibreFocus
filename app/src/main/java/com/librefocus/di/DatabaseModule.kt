package com.librefocus.di

import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.room.Room
import com.librefocus.data.local.database.UsageDatabase
import com.librefocus.data.local.UsageStatsProvider
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
    
    // Repository
    single {
        UsageTrackingRepository(
            usageStatsProvider = get(),
            appCategoryDao = get(),
            appDao = get(),
            hourlyAppUsageDao = get(),
            dailyDeviceUsageDao = get(),
            syncMetadataDao = get()
        )
    }
}
