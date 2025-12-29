package com.librefocus.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.librefocus.data.repository.BackupRestoreRepository
import com.librefocus.data.repository.PreferencesRepository
import com.librefocus.models.DateFormat
import com.librefocus.models.DateTimePreferences
import com.librefocus.models.TimeFormat
import com.librefocus.utils.DateTimeFormatterManager
import com.librefocus.utils.FormattedDateTimePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val dateTimeFormatterManager: DateTimeFormatterManager,
    private val backupRestoreRepository: BackupRestoreRepository
) : ViewModel() {

    val appTheme: StateFlow<String> = preferencesRepository.appTheme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "SYSTEM"
        )

    // Date & Time Settings State
    val dateTimePreferences: StateFlow<DateTimePreferences> = preferencesRepository.dateTimePreferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DateTimePreferences()
        )
    
    val formattedPreferences: StateFlow<FormattedDateTimePreferences?> = 
        dateTimeFormatterManager.formattedPreferences
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )
    
    // Backup/Restore/Reset State
    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState.asStateFlow()

    fun setAppTheme(theme: String) {
        viewModelScope.launch {
            preferencesRepository.setAppTheme(theme)
        }
    }

    // Date & Time Settings Methods
    
    fun setUseSystemDefaults(useSystem: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setUseSystemDateTime(useSystem)
        }
    }

    fun setTimeFormat(format: TimeFormat) {
        viewModelScope.launch {
            val current = dateTimePreferences.value
            preferencesRepository.setDateTimePreferences(
                current.copy(timeFormat = format)
            )
        }
    }

    fun setDateFormat(format: DateFormat) {
        viewModelScope.launch {
            preferencesRepository.setDateFormat(format)
        }
    }

    fun setTimeZone(zoneId: String?) {
        viewModelScope.launch {
            preferencesRepository.setTimeZoneId(zoneId)
        }
    }
    
    /**
     * Get available time zones grouped by region.
     */
    fun getAvailableTimeZones(): Map<String, List<String>> {
        return ZoneId.getAvailableZoneIds()
            .sorted()
            .groupBy { zoneId ->
                when {
                    zoneId.startsWith("America/") -> "Americas"
                    zoneId.startsWith("Europe/") -> "Europe"
                    zoneId.startsWith("Asia/") -> "Asia"
                    zoneId.startsWith("Africa/") -> "Africa"
                    zoneId.startsWith("Pacific/") -> "Pacific"
                    zoneId.startsWith("Australia/") -> "Australia"
                    zoneId == "UTC" -> "UTC"
                    else -> "Other"
                }
            }
    }
    
    // Backup/Restore/Reset Methods
    
    /**
     * Creates and exports a backup to the specified URI.
     */
    fun createAndExportBackup(uri: Uri) {
        viewModelScope.launch {
            _backupState.value = BackupState.InProgress("Creating backup...")
            
            val createResult = backupRestoreRepository.createBackup()
            if (createResult.isFailure) {
                _backupState.value = BackupState.Error(
                    createResult.exceptionOrNull()?.message ?: "Failed to create backup"
                )
                return@launch
            }
            
            val backupData = createResult.getOrThrow()
            val exportResult = backupRestoreRepository.exportBackup(uri, backupData)
            
            if (exportResult.isSuccess) {
                _backupState.value = BackupState.Success("Backup saved successfully")
            } else {
                _backupState.value = BackupState.Error(
                    exportResult.exceptionOrNull()?.message ?: "Failed to save backup"
                )
            }
        }
    }
    
    /**
     * Imports and restores a backup from the specified URI.
     * @param uri URI of the backup file
     * @param overrideConflicts If true, imported data overrides existing; if false, existing data is kept
     */
    fun importAndRestoreBackup(uri: Uri, overrideConflicts: Boolean) {
        viewModelScope.launch {
            _backupState.value = BackupState.InProgress("Importing backup...")
            
            val importResult = backupRestoreRepository.importBackup(uri)
            if (importResult.isFailure) {
                _backupState.value = BackupState.Error(
                    importResult.exceptionOrNull()?.message ?: "Failed to import backup"
                )
                return@launch
            }
            
            _backupState.value = BackupState.InProgress("Restoring data...")
            val backupData = importResult.getOrThrow()
            val restoreResult = backupRestoreRepository.restoreBackup(backupData, overrideConflicts)
            
            if (restoreResult.isSuccess) {
                _backupState.value = BackupState.Success("Backup restored successfully")
            } else {
                _backupState.value = BackupState.Error(
                    restoreResult.exceptionOrNull()?.message ?: "Failed to restore backup"
                )
            }
        }
    }
    
    /**
     * Resets all app data (database + preferences).
     */
    fun resetAllData() {
        viewModelScope.launch {
            _backupState.value = BackupState.InProgress("Resetting all data...")
            
            val result = backupRestoreRepository.resetAllData()
            
            if (result.isSuccess) {
                _backupState.value = BackupState.Success("All data has been reset")
            } else {
                _backupState.value = BackupState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to reset data"
                )
            }
        }
    }
    
    /**
     * Clears the backup state (dismisses success/error messages).
     */
    fun clearBackupState() {
        _backupState.value = BackupState.Idle
    }
}

sealed class BackupState {
    object Idle : BackupState()
    data class InProgress(val message: String) : BackupState()
    data class Success(val message: String) : BackupState()
    data class Error(val message: String) : BackupState()
}
