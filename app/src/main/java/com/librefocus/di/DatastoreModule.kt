package com.librefocus.di

import com.librefocus.data.local.PreferencesDataStore
import com.librefocus.data.repository.PreferencesRepository
import com.librefocus.utils.DateTimeFormatterManager
import org.koin.dsl.module
import java.util.Locale

val dataStoreModule = module {
    single { PreferencesDataStore(get()) }
    single { PreferencesRepository(get()) }
    single { 
        DateTimeFormatterManager(
            preferencesFlow = get<PreferencesRepository>().dateTimePreferences,
            locale = Locale.getDefault()
        )
    }
}
