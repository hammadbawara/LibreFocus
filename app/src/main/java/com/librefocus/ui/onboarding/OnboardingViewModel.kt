package com.librefocus.ui.onboarding

import android.Manifest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PermissionItem(
    val name: String,
    val iconRes: Int,
    val description: String,
    val permission: String,
    val isNecessary: Boolean,
    val isGranted: Boolean = false
)

data class OnboardingUiState(
    val permissions: List<PermissionItem> = emptyList()
)

class OnboardingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState

    init {
        loadPermissions()
    }

    private fun loadPermissions() {
        _uiState.value = OnboardingUiState(
            permissions = listOf(
                PermissionItem(
                    name = "Usage Access",
                    iconRes = android.R.drawable.ic_menu_info_details,
                    description = "Needed to monitor your app usage for insights.",
                    permission = "android.permission.PACKAGE_USAGE_STATS",
                    isNecessary = true
                ),
                PermissionItem(
                    name = "Notifications",
                    iconRes = android.R.drawable.ic_dialog_email,
                    description = "Used to track notification distractions.",
                    permission = Manifest.permission.POST_NOTIFICATIONS,
                    isNecessary = false
                ),
                PermissionItem(
                    name = "Battery Optimization",
                    iconRes = android.R.drawable.ic_lock_idle_charging,
                    description = "Required to run background tracking tasks.",
                    permission = Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    isNecessary = false
                )
            )
        )
    }

    fun updatePermissionStatus(permission: String, granted: Boolean) {
        viewModelScope.launch {
            val updated = _uiState.value.permissions.map {
                if (it.permission == permission) it.copy(isGranted = granted) else it
            }
            _uiState.value = _uiState.value.copy(permissions = updated)
        }
    }

    fun areNecessaryPermissionsGranted(): Boolean {
        return _uiState.value.permissions
            .filter { it.isNecessary }
            .all { it.isGranted }
    }
}
