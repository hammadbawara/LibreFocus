package com.librefocus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.librefocus.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    val repository: PreferencesRepository
) : ViewModel() {

    val onboardingShown: StateFlow<Boolean> = repository.onboardingShown
        .stateIn(viewModelScope, SharingStarted.Companion.Eagerly, true)

    fun setOnboardingShown(shown: Boolean) {
        viewModelScope.launch {
            repository.setOnboardingShown(shown)
        }
    }

}