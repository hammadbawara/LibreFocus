package com.librefocus.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.librefocus.models.DateFormat
import com.librefocus.models.DateTimePreferences
import com.librefocus.models.TimeFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val DATASTORE_NAME = "user_preferences"

val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

class PreferencesDataStore(private val context: Context) {

    private val ONBOARDING_SHOWN_KEY = booleanPreferencesKey("onboarding_shown")
    private val APP_THEME_KEY = stringPreferencesKey("app_theme")
    private val TIME_FORMAT_KEY = stringPreferencesKey("time_format")
    
    // Date & Time Preferences
    private val USE_SYSTEM_DATETIME_KEY = booleanPreferencesKey("use_system_datetime")
    private val DATE_FORMAT_KEY = stringPreferencesKey("date_format")
    private val TIME_ZONE_ID_KEY = stringPreferencesKey("time_zone_id")

    val onboardingShown: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ONBOARDING_SHOWN_KEY] ?: false
    }

    val appTheme: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[APP_THEME_KEY] ?: "SYSTEM"
    }

    val timeFormat: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[TIME_FORMAT_KEY] ?: "24H"
    }
    
    /**
     * Provides a Flow of DateTimePreferences combining all date/time-related preferences.
     */
    val dateTimePreferences: Flow<DateTimePreferences> = context.dataStore.data.map { prefs ->
        DateTimePreferences(
            useSystemDefaults = prefs[USE_SYSTEM_DATETIME_KEY] ?: true,
            timeFormat = TimeFormat.fromValue(prefs[TIME_FORMAT_KEY] ?: "SYSTEM"),
            dateFormat = DateFormat.fromValue(prefs[DATE_FORMAT_KEY] ?: "SYSTEM"),
            timeZoneId = prefs[TIME_ZONE_ID_KEY]
        )
    }

    suspend fun setOnboardingShown(shown: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[ONBOARDING_SHOWN_KEY] = shown
        }
    }

    suspend fun setAppTheme(theme: String) {
        context.dataStore.edit { prefs ->
            prefs[APP_THEME_KEY] = theme
        }
    }

    suspend fun setTimeFormat(format: String) {
        context.dataStore.edit { prefs ->
            prefs[TIME_FORMAT_KEY] = format
        }
    }
    
    /**
     * Sets whether to use system defaults for date/time formatting.
     */
    suspend fun setUseSystemDateTime(useSystem: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[USE_SYSTEM_DATETIME_KEY] = useSystem
        }
    }
    
    /**
     * Sets the date format preference.
     */
    suspend fun setDateFormat(format: DateFormat) {
        context.dataStore.edit { prefs ->
            prefs[DATE_FORMAT_KEY] = format.value
        }
    }
    
    /**
     * Sets the time zone ID preference.
     * Pass null or "SYSTEM" to use system default.
     */
    suspend fun setTimeZoneId(zoneId: String?) {
        context.dataStore.edit { prefs ->
            if (zoneId.isNullOrEmpty() || zoneId == "SYSTEM") {
                prefs.remove(TIME_ZONE_ID_KEY)
            } else {
                prefs[TIME_ZONE_ID_KEY] = zoneId
            }
        }
    }
    
    /**
     * Updates all date/time preferences at once.
     */
    suspend fun setDateTimePreferences(preferences: DateTimePreferences) {
        context.dataStore.edit { prefs ->
            prefs[USE_SYSTEM_DATETIME_KEY] = preferences.useSystemDefaults
            prefs[TIME_FORMAT_KEY] = preferences.timeFormat.value
            prefs[DATE_FORMAT_KEY] = preferences.dateFormat.value
            if (preferences.timeZoneId.isNullOrEmpty() || preferences.timeZoneId == "SYSTEM") {
                prefs.remove(TIME_ZONE_ID_KEY)
            } else {
                prefs[TIME_ZONE_ID_KEY] = preferences.timeZoneId
            }
        }
    }
}
