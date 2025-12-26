package com.librefocus.ui.limits

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.librefocus.data.local.database.mapper.createLaunchCountLimit
import com.librefocus.data.local.database.mapper.createScheduleLimit
import com.librefocus.data.local.database.mapper.createUsageLimit
import com.librefocus.data.repository.LimitRepository
import com.librefocus.models.DayOfWeek
import com.librefocus.models.Limit
import com.librefocus.models.ResetPeriod
import com.librefocus.models.TimeSlot
import com.librefocus.models.UsageLimitType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

data class ValidationErrors(
    val nameError: Boolean = false,
    val limitTypeError: Boolean = false,
    val appsError: Boolean = false
)

sealed class LimitConfiguration : Parcelable {
    @Parcelize
    data class Schedule(
        val isAllDay: Boolean,
        val timeSlots: List<TimeSlot>,
        val selectedDays: Set<DayOfWeek>
    ) : LimitConfiguration()

    @Parcelize
    data class Usage(
        val limitType: UsageLimitType,
        val durationMinutes: Int,
        val selectedDays: Set<DayOfWeek>
    ) : LimitConfiguration()

    @Parcelize
    data class LaunchCount(
        val maxLaunches: Int,
        val resetPeriod: ResetPeriod,
        val selectedDays: Set<DayOfWeek>
    ) : LimitConfiguration()
}

class CreateLimitViewModel(
    private val limitRepository: LimitRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val limitId: String? = savedStateHandle.get<String>("limitId")

    private val _limitNameState = MutableStateFlow("")
    val limitNameState: StateFlow<String> = _limitNameState.asStateFlow()

    private val _selectedAppsState = MutableStateFlow<List<String>>(emptyList())
    val selectedAppsState: StateFlow<List<String>> = _selectedAppsState.asStateFlow()

    private val _limitConfigurationState = MutableStateFlow<LimitConfiguration?>(null)
    val limitConfigurationState: StateFlow<LimitConfiguration?> = _limitConfigurationState.asStateFlow()

    private val _validationErrorsState = MutableStateFlow(ValidationErrors())
    val validationErrorsState: StateFlow<ValidationErrors> = _validationErrorsState.asStateFlow()

    private val _hasChangesState = MutableStateFlow(false)
    val hasChangesState: StateFlow<Boolean> = _hasChangesState.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    init {
        limitId?.let { loadLimit(it) }
        
        if (limitId == null) {
            viewModelScope.launch {
                _limitNameState.value = "Limit ${getNextLimitNumber()}"
            }
        }
    }

    private fun loadLimit(id: String) {
        viewModelScope.launch {
            limitRepository.getLimitById(id)?.let { limit ->
                _isEditMode.value = true
                _limitNameState.value = limit.name
                _selectedAppsState.value = limit.selectedAppPackages

                _limitConfigurationState.value = when (limit) {
                    is Limit.Schedule -> LimitConfiguration.Schedule(
                        isAllDay = limit.isAllDay,
                        timeSlots = limit.timeSlots,
                        selectedDays = limit.selectedDays
                    )
                    is Limit.UsageLimit -> LimitConfiguration.Usage(
                        limitType = limit.limitType,
                        durationMinutes = limit.durationMinutes,
                        selectedDays = limit.selectedDays
                    )
                    is Limit.LaunchCount -> LimitConfiguration.LaunchCount(
                        maxLaunches = limit.maxLaunches,
                        resetPeriod = limit.resetPeriod,
                        selectedDays = limit.selectedDays
                    )
                }
            }
        }
    }

    fun setLimitName(name: String) {
        _limitNameState.value = name
        _hasChangesState.value = true
    }

    fun setSelectedApps(packages: List<String>) {
        _selectedAppsState.value = packages
        _hasChangesState.value = true
    }

    fun setLimitConfiguration(config: LimitConfiguration) {
        _limitConfigurationState.value = config
        _hasChangesState.value = true
    }

    fun validateAndSave(): Boolean {
        val name = _limitNameState.value
        val apps = _selectedAppsState.value
        val config = _limitConfigurationState.value

        val errors = ValidationErrors(
            nameError = name.isBlank(),
            limitTypeError = config == null,
            appsError = apps.isEmpty()
        )

        _validationErrorsState.value = errors

        if (errors.nameError || errors.limitTypeError || errors.appsError) {
            return false
        }

        viewModelScope.launch {
            val limit = when (config) {
                is LimitConfiguration.Schedule -> createScheduleLimit(
                    name = name,
                    selectedAppPackages = apps,
                    isAllDay = config.isAllDay,
                    timeSlots = config.timeSlots,
                    selectedDays = config.selectedDays
                )
                is LimitConfiguration.Usage -> createUsageLimit(
                    name = name,
                    selectedAppPackages = apps,
                    limitType = config.limitType,
                    durationMinutes = config.durationMinutes,
                    selectedDays = config.selectedDays
                )
                is LimitConfiguration.LaunchCount -> createLaunchCountLimit(
                    name = name,
                    selectedAppPackages = apps,
                    maxLaunches = config.maxLaunches,
                    resetPeriod = config.resetPeriod,
                    selectedDays = config.selectedDays
                )
                null -> return@launch
            }

            if (_isEditMode.value && limitId != null) {
                val updatedLimit = when (limit) {
                    is Limit.Schedule -> limit.copy(id = limitId)
                    is Limit.UsageLimit -> limit.copy(id = limitId)
                    is Limit.LaunchCount -> limit.copy(id = limitId)
                }
                limitRepository.updateLimit(updatedLimit)
            } else {
                limitRepository.insertLimit(limit)
            }

            _hasChangesState.value = false
        }

        return true
    }

    suspend fun getNextLimitNumber(): Int {
        return limitRepository.getLimitCount().let { flow ->
            var count = 0
            flow.collect { count = it }
            count + 1
        }
    }

    fun clearValidationErrors() {
        _validationErrorsState.value = ValidationErrors()
    }
}
