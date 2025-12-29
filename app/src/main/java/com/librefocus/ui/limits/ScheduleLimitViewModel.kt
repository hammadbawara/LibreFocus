package com.librefocus.ui.limits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.librefocus.data.repository.PreferencesRepository
import com.librefocus.models.DateTimePreferences
import com.librefocus.models.DayOfWeek
import com.librefocus.models.TimeSlot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class TimeSlotValidationError(
    val message: String
)

class ScheduleLimitViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val dateTimePreferences: StateFlow<DateTimePreferences> = preferencesRepository.dateTimePreferences
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = DateTimePreferences()
        )

    private val _isAllDayState = MutableStateFlow(true)
    val isAllDayState: StateFlow<Boolean> = _isAllDayState.asStateFlow()

    private val _timeSlotsState = MutableStateFlow<List<TimeSlot>>(emptyList())
    val timeSlotsState: StateFlow<List<TimeSlot>> = _timeSlotsState.asStateFlow()

    private val _selectedDaysState = MutableStateFlow<Set<DayOfWeek>>(DayOfWeek.values().toSet())
    val selectedDaysState: StateFlow<Set<DayOfWeek>> = _selectedDaysState.asStateFlow()

    private val _isAllWeekState = MutableStateFlow(true)
    val isAllWeekState: StateFlow<Boolean> = _isAllWeekState.asStateFlow()

    private val _validationErrorState = MutableStateFlow<TimeSlotValidationError?>(null)
    val validationErrorState: StateFlow<TimeSlotValidationError?> = _validationErrorState.asStateFlow()

    val isSaveEnabled: StateFlow<Boolean> = combine(
        _isAllDayState,
        _timeSlotsState,
        _isAllWeekState,
        _selectedDaysState
    ) { isAllDay, timeSlots, isAllWeek, selectedDays ->
        val hasDays = isAllWeek || selectedDays.isNotEmpty()
        val hasTimeSlots = isAllDay || timeSlots.isNotEmpty()
        hasDays && hasTimeSlots
    }.stateIn(
        scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun setAllDay(isAllDay: Boolean) {
        _isAllDayState.value = isAllDay
    }

    fun validateAndAddTimeSlot(fromHour: Int, toHour: Int): Boolean {
        val fromMinutes = fromHour * 60
        val toMinutes = toHour * 60

        if (fromMinutes >= toMinutes) {
            _validationErrorState.value = TimeSlotValidationError("End time must be after start time")
            return false
        }

        val newSlot = TimeSlot(fromHour = fromMinutes, toHour = toMinutes)
        val existingSlots = _timeSlotsState.value

        if (existingSlots.any { it.fromHour == newSlot.fromHour && it.toHour == newSlot.toHour }) {
            _validationErrorState.value = TimeSlotValidationError("This hour slot is already added")
            return false
        }

        val hasOverlap = existingSlots.any { existing ->
            (newSlot.fromHour < existing.toHour && newSlot.toHour > existing.fromHour)
        }

        if (hasOverlap) {
            _validationErrorState.value = TimeSlotValidationError("This time slot overlaps with an existing slot")
            return false
        }

        _timeSlotsState.value = existingSlots + newSlot
        _validationErrorState.value = null
        return true
    }

    fun removeTimeSlot(index: Int) {
        _timeSlotsState.value = _timeSlotsState.value.filterIndexed { i, _ -> i != index }
    }

    fun clearValidationError() {
        _validationErrorState.value = null
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

    fun saveScheduleLimit(): LimitConfiguration.Schedule {
        return LimitConfiguration.Schedule(
            isAllDay = _isAllDayState.value,
            timeSlots = _timeSlotsState.value,
            selectedDays = _selectedDaysState.value
        )
    }
}
