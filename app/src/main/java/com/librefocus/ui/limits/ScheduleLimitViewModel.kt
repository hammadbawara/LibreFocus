package com.librefocus.ui.limits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.librefocus.models.DayOfWeek
import com.librefocus.models.TimeSlot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScheduleLimitViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _isAllDayState = MutableStateFlow(false)
    val isAllDayState: StateFlow<Boolean> = _isAllDayState.asStateFlow()

    private val _timeSlotsState = MutableStateFlow<List<TimeSlot>>(emptyList())
    val timeSlotsState: StateFlow<List<TimeSlot>> = _timeSlotsState.asStateFlow()

    private val _selectedDaysState = MutableStateFlow<Set<DayOfWeek>>(DayOfWeek.values().toSet())
    val selectedDaysState: StateFlow<Set<DayOfWeek>> = _selectedDaysState.asStateFlow()

    private val _isAllWeekState = MutableStateFlow(true)
    val isAllWeekState: StateFlow<Boolean> = _isAllWeekState.asStateFlow()

    fun setAllDay(isAllDay: Boolean) {
        _isAllDayState.value = isAllDay
        if (isAllDay) {
            _timeSlotsState.value = emptyList()
        }
    }

    fun addTimeSlot(fromHour: Int, fromMinute: Int, toHour: Int, toMinute: Int) {
        val newSlot = TimeSlot(
            fromHour = fromHour * 60 + fromMinute,
            toHour = toHour * 60 + toMinute
        )
        _timeSlotsState.value = _timeSlotsState.value + newSlot
    }

    fun removeTimeSlot(index: Int) {
        _timeSlotsState.value = _timeSlotsState.value.filterIndexed { i, _ -> i != index }
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

    fun saveScheduleLimit(): LimitConfiguration.Schedule {
        return LimitConfiguration.Schedule(
            isAllDay = _isAllDayState.value,
            timeSlots = _timeSlotsState.value,
            selectedDays = _selectedDaysState.value
        )
    }
}
