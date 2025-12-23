package com.librefocus

import android.app.Application
import com.librefocus.di.appDetailModule
import com.librefocus.di.categoryModule
import com.librefocus.di.dataStoreModule
import com.librefocus.di.databaseModule
import com.librefocus.di.homeModule
import com.librefocus.di.mainModule
import com.librefocus.di.onboardingModule
import com.librefocus.di.settingsModule
import com.librefocus.di.statsModule
import com.librefocus.di.workerModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class LibreFocus: Application() {
    override fun onCreate() {
        super.onCreate()

        // Initializing Koin for dependency injection
        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@LibreFocus)
            modules(
                onboardingModule,
                dataStoreModule,
                databaseModule,
                mainModule,
                homeModule,
                statsModule,
                settingsModule,
                appDetailModule,
                categoryModule,
                workerModule
            )
        }
    }
}