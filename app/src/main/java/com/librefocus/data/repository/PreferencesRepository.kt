package com.librefocus.data.repository

import com.librefocus.data.local.PreferencesDataStore
import com.librefocus.models.DateFormat
import com.librefocus.models.DateTimePreferences
import kotlinx.coroutines.flow.Flow

class PreferencesRepository(private val dataStore: PreferencesDataStore) {

    val onboardingShown: Flow<Boolean> = dataStore.onboardingShown
    val appTheme: Flow<String> = dataStore.appTheme
    val timeFormat: Flow<String> = dataStore.timeFormat
    
    /**
     * Provides a Flow of combined date/time preferences.
     */
    val dateTimePreferences: Flow<DateTimePreferences> = dataStore.dateTimePreferences

    suspend fun setOnboardingShown(shown: Boolean) {
        dataStore.setOnboardingShown(shown)
    }

    suspend fun setAppTheme(theme: String) {
        dataStore.setAppTheme(theme)
    }

    suspend fun setTimeFormat(format: String) {
        dataStore.setTimeFormat(format)
    }
    
    /**
     * Sets whether to use system defaults for date/time formatting.
     */
    suspend fun setUseSystemDateTime(useSystem: Boolean) {
        dataStore.setUseSystemDateTime(useSystem)
    }
    
    /**
     * Sets the date format preference.
     */
    suspend fun setDateFormat(format: DateFormat) {
        dataStore.setDateFormat(format)
    }
    
    /**
     * Sets the time zone ID preference.
     */
    suspend fun setTimeZoneId(zoneId: String?) {
        dataStore.setTimeZoneId(zoneId)
    }
    
    /**
     * Updates all date/time preferences at once.
     */
    suspend fun setDateTimePreferences(preferences: DateTimePreferences) {
        dataStore.setDateTimePreferences(preferences)
    }
}