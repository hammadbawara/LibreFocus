package com.librefocus.data.repository

import com.librefocus.data.local.PreferencesDataStore
import kotlinx.coroutines.flow.Flow

class PreferencesRepository(private val dataStore: PreferencesDataStore) {

    val onboardingShown: Flow<Boolean> = dataStore.onboardingShown
    val appTheme: Flow<String> = dataStore.appTheme
    val timeFormat: Flow<String> = dataStore.timeFormat

    suspend fun setOnboardingShown(shown: Boolean) {
        dataStore.setOnboardingShown(shown)
    }

    suspend fun setAppTheme(theme: String) {
        dataStore.setAppTheme(theme)
    }

    suspend fun setTimeFormat(format: String) {
        dataStore.setTimeFormat(format)
    }
}