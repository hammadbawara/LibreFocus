package com.librefocus.ui.limits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.librefocus.models.DayOfWeek
import com.librefocus.ui.common.AppScaffold
import org.koin.androidx.compose.koinViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleLimitScreen(
    onNavigateBack: (LimitConfiguration.Schedule?) -> Unit,
    viewModel: ScheduleLimitViewModel = koinViewModel()
) {
    val isAllDay by viewModel.isAllDayState.collectAsStateWithLifecycle()
    val timeSlots by viewModel.timeSlotsState.collectAsStateWithLifecycle()
    val selectedDays by viewModel.selectedDaysState.collectAsStateWithLifecycle()
    val isAllWeek by viewModel.isAllWeekState.collectAsStateWithLifecycle()

    var showTimePicker by remember { mutableStateOf(false) }
    var timePickerType by remember { mutableStateOf<TimePickerType?>(null) }
    var pendingFromTime by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    AppScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schedule Limit") },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack(null) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues, _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "All Day",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Checkbox(
                        checked = isAllDay,
                        onCheckedChange = { viewModel.setAllDay(it) }
                    )
                }
            }

            if (!isAllDay) {
                Text(
                    text = "Time Slots",
                    style = MaterialTheme.typography.titleSmall
                )

                timeSlots.forEachIndexed { index, slot ->
                    OutlinedCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${formatMinutesToTime(slot.fromHour)} - ${formatMinutesToTime(slot.toHour)}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            IconButton(onClick = { viewModel.removeTimeSlot(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        timePickerType = TimePickerType.FROM
                        showTimePicker = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Time Slot")
                }
            }

            Card {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "All Week",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Checkbox(
                        checked = isAllWeek,
                        onCheckedChange = { viewModel.setAllWeek(it) }
                    )
                }
            }

            DayChipsRow(
                selectedDays = selectedDays,
                onDayToggle = { viewModel.toggleDay(it) }
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val config = viewModel.saveScheduleLimit()
                    onNavigateBack(config)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }

    if (showTimePicker && timePickerType != null) {
        TimePickerDialog(
            title = if (timePickerType == TimePickerType.FROM) "Select Start Time" else "Select End Time",
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                when (timePickerType) {
                    TimePickerType.FROM -> {
                        pendingFromTime = Pair(hour, minute)
                        timePickerType = TimePickerType.TO
                    }
                    TimePickerType.TO -> {
                        pendingFromTime?.let { (fromHour, fromMinute) ->
                            viewModel.addTimeSlot(fromHour, fromMinute, hour, minute)
                        }
                        pendingFromTime = null
                        timePickerType = null
                        showTimePicker = false
                    }
                    null -> {}
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val timePickerState = rememberTimePickerState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            TimePicker(state = timePickerState)
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(timePickerState.hour, timePickerState.minute)
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DayChipsRow(
    selectedDays: Set<DayOfWeek>,
    onDayToggle: (DayOfWeek) -> Unit
) {
    val days = listOf(
        DayOfWeek.MON to "Mon",
        DayOfWeek.TUE to "Tue",
        DayOfWeek.WED to "Wed",
        DayOfWeek.THU to "Thu",
        DayOfWeek.FRI to "Fri",
        DayOfWeek.SAT to "Sat",
        DayOfWeek.SUN to "Sun"
    )

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        days.forEach { (day, label) ->
            FilterChip(
                selected = day in selectedDays,
                onClick = { onDayToggle(day) },
                label = { Text(label) }
            )
        }
    }
}

private enum class TimePickerType {
    FROM, TO
}

private fun formatMinutesToTime(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val time = LocalTime.of(hours, minutes)
    return time.format(DateTimeFormatter.ofPattern("hh:mm a"))
}
