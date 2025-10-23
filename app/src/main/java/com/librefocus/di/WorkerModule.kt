package com.librefocus.di

import androidx.work.WorkManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module for WorkManager.
 * Note: Workers are created by WorkManager framework, not Koin,
 * so we only provide the WorkManager instance.
 */
val workerModule = module {
    // WorkManager instance
    single { WorkManager.getInstance(androidContext()) }
}