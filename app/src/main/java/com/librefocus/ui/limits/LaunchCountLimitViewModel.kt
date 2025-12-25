package com.librefocus.ui.limits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.librefocus.models.DayOfWeek
import com.librefocus.models.ResetPeriod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

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

    private val _validationErrorState = MutableStateFlow<String?>(null)
    val validationErrorState: StateFlow<String?> = _validationErrorState.asStateFlow()

    val isSaveEnabled: StateFlow<Boolean> = combine(
        _maxLaunchesState,
        _selectedDaysState
    ) { maxLaunches, selectedDays ->
        maxLaunches > 0 && selectedDays.isNotEmpty()
    }.stateIn(
        scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun setMaxLaunches(count: Int) {
        _maxLaunchesState.value = count.coerceAtLeast(1)
    }

    fun incrementLaunches() {
        _maxLaunchesState.value = (_maxLaunchesState.value + 1).coerceAtMost(999)
    }

    fun decrementLaunches() {
        _maxLaunchesState.value = (_maxLaunchesState.value - 1).coerceAtLeast(1)
    }

    fun setResetPeriod(period: ResetPeriod) {
        _resetPeriodState.value = period
    }

    fun setAllWeek(isAllWeek: Boolean) {
        _isAllWeekState.value = isAllWeek
        if (isAllWeek) {
            _selectedDaysState.value = DayOfWeek.entries.toSet()
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

    fun validateAndSave(): LimitConfiguration.LaunchCount? {
        if (_selectedDaysState.value.isEmpty()) {
            _validationErrorState.value = "Please select at least one weekday"
            return null
        }
        if (_maxLaunchesState.value <= 0) {
            _validationErrorState.value = "Launch count must be greater than zero"
            return null
        }
        
        return LimitConfiguration.LaunchCount(
            maxLaunches = _maxLaunchesState.value,
            resetPeriod = _resetPeriodState.value,
            selectedDays = _selectedDaysState.value
        )
    }
}
