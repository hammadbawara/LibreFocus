package com.librefocus.ui.onboarding

import android.Manifest
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
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

sealed class PermissionEvent {
    data class RequestPermission(val permission: String) : PermissionEvent()
    object RequestUsageAccess : PermissionEvent()
    object RequestIgnoreBatteryOptimization : PermissionEvent()
    data class OpenAppInfo(val permission: String) : PermissionEvent()
}

class OnboardingViewModel() : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState

    private val _permissionEvents = MutableSharedFlow<PermissionEvent>()
    val permissionEvents: SharedFlow<PermissionEvent> = _permissionEvents

    init {
        loadPermissions()
    }

    private fun loadPermissions() {
        val perms = mutableListOf(
            PermissionItem(
                name = "Usage Access",
                iconRes = android.R.drawable.ic_menu_info_details,
                description = "Needed to monitor your app usage for insights.",
                permission = "android.permission.PACKAGE_USAGE_STATS",
                isNecessary = true
            )
        )

        // Add notification permission only on Android 13+ to avoid API-level warnings
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            perms.add(
                PermissionItem(
                    name = "Notifications",
                    iconRes = android.R.drawable.ic_dialog_info,
                    description = "Used to track notification distractions.",
                    permission = Manifest.permission.POST_NOTIFICATIONS,
                    isNecessary = false
                )
            )
        }

        // Battery optimization "permission" is handled via settings intent, not runtime permission
        perms.add(
            PermissionItem(
                name = "Battery Optimization",
                iconRes = android.R.drawable.ic_lock_idle_charging,
                description = "Required to run background tracking tasks.",
                permission = Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                isNecessary = false
            )
        )

        _uiState.value = OnboardingUiState(permissions = perms)
    }

    fun onPermissionRequestClicked(permission: String) {
        viewModelScope.launch {
            when (permission) {
                "android.permission.PACKAGE_USAGE_STATS" -> _permissionEvents.emit(PermissionEvent.RequestUsageAccess)
                Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS -> _permissionEvents.emit(PermissionEvent.RequestIgnoreBatteryOptimization)
                else -> _permissionEvents.emit(PermissionEvent.RequestPermission(permission))
            }
        }
    }

    fun onPermissionDeniedPermanently(permission: String) {
        viewModelScope.launch {
            _permissionEvents.emit(PermissionEvent.OpenAppInfo(permission))
        }
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

    fun checkUsageAccessGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.checkOpNoThrow(
            "android:get_usage_stats",
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }
}
