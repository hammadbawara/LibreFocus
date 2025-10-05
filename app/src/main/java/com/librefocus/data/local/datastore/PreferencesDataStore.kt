package com.librefocus.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val DATASTORE_NAME = "user_preferences"

val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

class PreferencesDataStore(private val context: Context) {

    private val ONBOARDING_SHOWN_KEY = booleanPreferencesKey("onboarding_shown")

    val onboardingShown: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ONBOARDING_SHOWN_KEY] ?: false
    }

    suspend fun setOnboardingShown(shown: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[ONBOARDING_SHOWN_KEY] = shown
        }
    }
}
