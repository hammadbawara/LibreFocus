package com.librefocus.data.repository

import com.librefocus.data.local.PreferencesDataStore
import com.librefocus.models.DateFormat
import com.librefocus.models.DateTimePreferences
import kotlinx.coroutines.flow.Flow

class PreferencesRepository(private val dataStore: PreferencesDataStore) {

    val onboardingShown: Flow<Boolean> = dataStore.onboardingShown
    val appTheme: Flow<String> = dataStore.appTheme
    val timeFormat: Flow<String> = dataStore.timeFormat
    val dailyScreenTimeGoalMinutes: Flow<Int> = dataStore.dailyScreenTimeGoalMinutes

    /** Flow that emits the last open conversation id (nullable) */
    val lastConversationId: Flow<String?> = dataStore.lastConversationId

    suspend fun setLastConversationId(id: String?) {
        dataStore.setLastConversationId(id)
    }

    // Conversation title helpers
    suspend fun setConversationTitle(conversationId: String, title: String?) {
        dataStore.setConversationTitle(conversationId, title)
    }

    fun getConversationTitle(conversationId: String): Flow<String?> = dataStore.getConversationTitle(conversationId)

    // Conversation provider/model helpers
    suspend fun setConversationProvider(conversationId: String, provider: String?) {
        dataStore.setConversationProvider(conversationId, provider)
    }

    fun getConversationProvider(conversationId: String): Flow<String?> = dataStore.getConversationProvider(conversationId)

    suspend fun setConversationModel(conversationId: String, model: String?) {
        dataStore.setConversationModel(conversationId, model)
    }

    fun getConversationModel(conversationId: String): Flow<String?> = dataStore.getConversationModel(conversationId)

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

    suspend fun setDailyScreenTimeGoalMinutes(minutes: Int) {
        dataStore.setDailyScreenTimeGoalMinutes(minutes)
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