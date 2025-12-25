package com.librefocus.ui.limits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.librefocus.models.DayOfWeek
import com.librefocus.models.UsageLimitType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UsageLimitViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _limitTypeState = MutableStateFlow(UsageLimitType.DAILY)
    val limitTypeState: StateFlow<UsageLimitType> = _limitTypeState.asStateFlow()

    private val _hoursState = MutableStateFlow(0)
    val hoursState: StateFlow<Int> = _hoursState.asStateFlow()

    private val _minutesState = MutableStateFlow(15)
    val minutesState: StateFlow<Int> = _minutesState.asStateFlow()

    private val _selectedDaysState = MutableStateFlow<Set<DayOfWeek>>(DayOfWeek.values().toSet())
    val selectedDaysState: StateFlow<Set<DayOfWeek>> = _selectedDaysState.asStateFlow()

    private val _isAllWeekState = MutableStateFlow(true)
    val isAllWeekState: StateFlow<Boolean> = _isAllWeekState.asStateFlow()

    fun setLimitType(type: UsageLimitType) {
        _limitTypeState.value = type
    }

    fun setTime(hours: Int, minutes: Int) {
        _hoursState.value = hours
        _minutesState.value = minutes
    }

    fun setAllWeek(isAllWeek: Boolean) {
        _isAllWeekState.value = isAllWeek
        _selectedDaysState.value = if (isAllWeek) {
            DayOfWeek.values().toSet()
        } else {
            emptySet()
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

    fun saveUsageLimit(): LimitConfiguration.Usage {
        val totalMinutes = (_hoursState.value * 60) + _minutesState.value
        return LimitConfiguration.Usage(
            limitType = _limitTypeState.value,
            durationMinutes = totalMinutes,
            selectedDays = _selectedDaysState.value
        )
    }
}
