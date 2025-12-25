package com.librefocus.ui.limits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.librefocus.data.repository.LimitRepository
import com.librefocus.models.Limit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LimitsViewModel(
    private val limitRepository: LimitRepository
) : ViewModel() {

    val limitsState: StateFlow<List<Limit>> = limitRepository.getAllLimits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleLimitEnabled(limitId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            limitRepository.toggleLimitEnabled(limitId, isEnabled)
        }
    }

    fun deleteLimit(limitId: String) {
        viewModelScope.launch {
            limitRepository.deleteLimit(limitId)
        }
    }
}
