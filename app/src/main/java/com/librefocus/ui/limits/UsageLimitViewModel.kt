package com.librefocus.ui.limits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.librefocus.models.DayOfWeek
import com.librefocus.models.UsageLimitType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class UsageLimitViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _limitTypeState = MutableStateFlow(UsageLimitType.DAILY)
    val limitTypeState: StateFlow<UsageLimitType> = _limitTypeState.asStateFlow()

    private val _minutesState = MutableStateFlow(60)
    val minutesState: StateFlow<Int> = _minutesState.asStateFlow()

    private val _minutesInputState = MutableStateFlow("60")
    val minutesInputState: StateFlow<String> = _minutesInputState.asStateFlow()

    private val _selectedDaysState = MutableStateFlow<Set<DayOfWeek>>(DayOfWeek.values().toSet())
    val selectedDaysState: StateFlow<Set<DayOfWeek>> = _selectedDaysState.asStateFlow()

    private val _isAllWeekState = MutableStateFlow(true)
    val isAllWeekState: StateFlow<Boolean> = _isAllWeekState.asStateFlow()

    private val _validationErrorState = MutableStateFlow<String?>(null)
    val validationErrorState: StateFlow<String?> = _validationErrorState.asStateFlow()

    val isSaveEnabled: StateFlow<Boolean> = combine(
        _minutesState,
        _selectedDaysState
    ) { minutes, selectedDays ->
        minutes > 0 && selectedDays.isNotEmpty()
    }.stateIn(
        scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun setLimitType(type: UsageLimitType) {
        _limitTypeState.value = type
        // Reset to default when switching types
        if (type == UsageLimitType.HOURLY) {
            setMinutesInput("30")
        } else {
            setMinutesInput("60")
        }
    }

    fun setMinutesInput(input: String) {
        _minutesInputState.value = input
        val value = input.toIntOrNull() ?: 0
        
        when (_limitTypeState.value) {
            UsageLimitType.HOURLY -> {
                // Hourly: 0-59 minutes only
                if (value in 0..59) {
                    _minutesState.value = value
                    _validationErrorState.value = null
                } else if (value > 59) {
                    _validationErrorState.value = "Hourly limit must be between 0-59 minutes"
                }
            }
            UsageLimitType.DAILY -> {
                // Daily: any positive number of minutes
                if (value > 0) {
                    _minutesState.value = value
                    _validationErrorState.value = null
                }
            }
        }
    }

    fun setAllWeek(isAllWeek: Boolean) {
        _isAllWeekState.value = isAllWeek
        if (isAllWeek) {
            _selectedDaysState.value = DayOfWeek.values().toSet()
        }
    }

    fun toggleDay(day: DayOfWeek) {
        val currentDays = _selectedDaysState.value.toMutableSet()
        if (day in currentDays) {
            currentDays.remove(day)
        } else {
            currentDays.add(day)
        }
        _selectedDaysState.value = currentDays
        _isAllWeekState.value = currentDays.size == 7
    }

    fun clearValidationError() {
        _validationErrorState.value = null
    }

    fun validateAndSave(): LimitConfiguration.Usage? {
        if (_selectedDaysState.value.isEmpty()) {
            _validationErrorState.value = "Please select at least one weekday"
            return null
        }
        if (_minutesState.value <= 0) {
            _validationErrorState.value = "Please enter a valid time limit"
            return null
        }
        
        return LimitConfiguration.Usage(
            limitType = _limitTypeState.value,
            durationMinutes = _minutesState.value,
            selectedDays = _selectedDaysState.value
        )
    }
}
