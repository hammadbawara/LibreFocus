package com.librefocus.di

import com.librefocus.data.local.PreferencesDataStore
import com.librefocus.data.repository.PreferencesRepository
import org.koin.dsl.module

val dataStoreModule = module {
    single { PreferencesDataStore(get()) }
    single { PreferencesRepository(get()) }
}
