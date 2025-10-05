package com.librefocus.data.respository

import com.librefocus.data.local.datastore.PreferencesDataStore
import kotlinx.coroutines.flow.Flow

class PreferencesRepository(private val dataStore: PreferencesDataStore) {

    val onboardingShown: Flow<Boolean> = dataStore.onboardingShown

    suspend fun setOnboardingShown(shown: Boolean) {
        dataStore.setOnboardingShown(shown)
    }
}