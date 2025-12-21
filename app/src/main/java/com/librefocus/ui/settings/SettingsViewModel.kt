package com.librefocus.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.librefocus.data.repository.PreferencesRepository
import com.librefocus.models.DateFormat
import com.librefocus.models.DateTimePreferences
import com.librefocus.models.TimeFormat
import com.librefocus.utils.DateTimeFormatterManager
import com.librefocus.utils.FormattedDateTimePreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val dateTimeFormatterManager: DateTimeFormatterManager
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
}
