package com.librefocus.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.librefocus.data.repository.UsageRepository
import com.librefocus.models.AppUsage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val apps: List<AppUsage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class HomeViewModel(
    private val repository: UsageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadAppUsage()
    }

    fun loadAppUsage() {
        viewModelScope.launch {
            _uiState.value = HomeUiState(isLoading = true)
            try {
                val apps = repository.getTodayAppUsage()
                _uiState.value = HomeUiState(apps = apps, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = HomeUiState(isLoading = false, error = e.message)
            }
        }
    }
}
