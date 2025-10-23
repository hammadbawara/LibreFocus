package com.librefocus.di

import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.room.Room
import com.librefocus.data.local.database.UsageDatabase
import com.librefocus.data.local.datasource.UsageStatsDataSource
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
            .fallbackToDestructiveMigration(false)
            .build()
    }
    
    // DAOs
    single { get<UsageDatabase>().appCategoryDao() }
    single { get<UsageDatabase>().appDao() }
    single { get<UsageDatabase>().hourlyAppUsageDao() }
    single { get<UsageDatabase>().syncMetadataDao() }
    
    // UsageStatsManager
    single {
        get<Context>().getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }
    
    // Data Source
    single {
        UsageStatsDataSource(
            context = get(),
            usageStatsManager = get()
        )
    }
    
    // Repository
    single {
        UsageTrackingRepository(
            usageStatsDataSource = get(),
            appCategoryDao = get(),
            appDao = get(),
            hourlyAppUsageDao = get(),
            syncMetadataDao = get()
        )
    }
}
