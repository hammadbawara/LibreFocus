package com.librefocus.ui.limits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.librefocus.models.DayOfWeek
import com.librefocus.models.ResetPeriod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LaunchCountLimitViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _maxLaunchesState = MutableStateFlow(5)
    val maxLaunchesState: StateFlow<Int> = _maxLaunchesState.asStateFlow()

    private val _resetPeriodState = MutableStateFlow(ResetPeriod.DAILY)
    val resetPeriodState: StateFlow<ResetPeriod> = _resetPeriodState.asStateFlow()

    private val _selectedDaysState = MutableStateFlow(DayOfWeek.entries.toSet())
    val selectedDaysState: StateFlow<Set<DayOfWeek>> = _selectedDaysState.asStateFlow()

    private val _isAllWeekState = MutableStateFlow(true)
    val isAllWeekState: StateFlow<Boolean> = _isAllWeekState.asStateFlow()

    fun setMaxLaunches(count: Int) {
        _maxLaunchesState.value = count.coerceAtLeast(0)
    }

    fun setResetPeriod(period: ResetPeriod) {
        _resetPeriodState.value = period
    }

    fun setAllWeek(isAllWeek: Boolean) {
        _isAllWeekState.value = isAllWeek
        _selectedDaysState.value = if (isAllWeek) {
            DayOfWeek.entries.toSet()
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

    fun saveLaunchCountLimit(): LimitConfiguration.LaunchCount {
        return LimitConfiguration.LaunchCount(
            maxLaunches = _maxLaunchesState.value,
            resetPeriod = _resetPeriodState.value,
            selectedDays = _selectedDaysState.value
        )
    }
}
