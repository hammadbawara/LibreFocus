package com.librefocus

import android.app.Application
import com.librefocus.di.onboardingModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class LibreFocus: Application() {
    override fun onCreate() {
        super.onCreate()

        // Initializing Koin for dependency injection
        startKoin {
            androidContext(this@LibreFocus)
            modules(onboardingModule)
        }
    }
}