package com.librefocus.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.librefocus.data.repository.UsageTrackingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SyncUiState(
    val isSyncing: Boolean = false,
    val lastSyncTime: Long? = null,
    val error: String? = null
)

class UsageSyncViewModel(
    private val repository: UsageTrackingRepository
) : ViewModel() {

    private val _syncState = MutableStateFlow(SyncUiState())
    val syncState: StateFlow<SyncUiState> = _syncState

    fun syncUsageStats(forceFullSync: Boolean = false) {
        viewModelScope.launch {
            try {
                _syncState.value = _syncState.value.copy(isSyncing = true, error = null)

                // Run the sync operation
                repository.syncUsageStats(forceFullSync)

                // Update state on success
                _syncState.value = _syncState.value.copy(
                    isSyncing = false,
                    lastSyncTime = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                _syncState.value = _syncState.value.copy(
                    isSyncing = false,
                    error = e.message ?: "Sync failed"
                )
            }
        }
    }
}