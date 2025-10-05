package com.librefocus.data.local.datastore

import kotlinx.coroutines.flow.Flow

class PreferencesRepository(private val dataStore: PreferencesDataStore) {

    val onboardingShown: Flow<Boolean> = dataStore.onboardingShown

    suspend fun setOnboardingShown(shown: Boolean) {
        dataStore.setOnboardingShown(shown)
    }
}
