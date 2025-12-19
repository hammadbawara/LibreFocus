package com.librefocus.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val DATASTORE_NAME = "user_preferences"

val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

class PreferencesDataStore(private val context: Context) {

    private val ONBOARDING_SHOWN_KEY = booleanPreferencesKey("onboarding_shown")
    private val APP_THEME_KEY = stringPreferencesKey("app_theme")
    private val TIME_FORMAT_KEY = stringPreferencesKey("time_format")

    val onboardingShown: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ONBOARDING_SHOWN_KEY] ?: false
    }

    val appTheme: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[APP_THEME_KEY] ?: "SYSTEM"
    }

    val timeFormat: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[TIME_FORMAT_KEY] ?: "24H"
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
}
