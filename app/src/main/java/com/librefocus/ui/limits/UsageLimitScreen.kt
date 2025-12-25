package com.librefocus.ui.limits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.commandiron.wheel_picker_compose.WheelTimePicker
import com.commandiron.wheel_picker_compose.core.TimeFormat
import com.librefocus.models.UsageLimitType
import com.librefocus.ui.common.AppScaffold
import org.koin.androidx.compose.koinViewModel
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageLimitScreen(
    onNavigateBack: (LimitConfiguration.Usage?) -> Unit,
    viewModel: UsageLimitViewModel = koinViewModel()
) {
    val limitType by viewModel.limitTypeState.collectAsStateWithLifecycle()
    val hours by viewModel.hoursState.collectAsStateWithLifecycle()
    val minutes by viewModel.minutesState.collectAsStateWithLifecycle()
    val selectedDays by viewModel.selectedDaysState.collectAsStateWithLifecycle()
    val isAllWeek by viewModel.isAllWeekState.collectAsStateWithLifecycle()

    AppScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Usage Limit") },
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
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Duration Type",
                    style = MaterialTheme.typography.titleSmall
                )

                Card {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = limitType == UsageLimitType.DAILY,
                                onClick = { viewModel.setLimitType(UsageLimitType.DAILY) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Daily")
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = limitType == UsageLimitType.HOURLY,
                                onClick = { viewModel.setLimitType(UsageLimitType.HOURLY) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Hourly")
                        }
                    }
                }

                Text(
                    text = "Time Limit",
                    style = MaterialTheme.typography.titleSmall
                )

                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WheelTimePicker(
                            startTime = LocalTime.of(hours, minutes),
                            timeFormat = TimeFormat.HOUR_24,
                            size = DpSize(200.dp, 150.dp),
                            textStyle = MaterialTheme.typography.titleMedium,
                            onSnappedTime = { time ->
                                viewModel.setTime(time.hour, time.minute)
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = formatDuration(hours, minutes, limitType),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                DaysSelectionCard(
                    isAllWeek = isAllWeek,
                    selectedDays = selectedDays,
                    onAllWeekChange = { viewModel.setAllWeek(it) },
                    onDayToggle = { viewModel.toggleDay(it) }
                )
            }

            Button(
                onClick = {
                    val config = viewModel.saveUsageLimit()
                    onNavigateBack(config)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Save")
            }
        }
    }
}

private fun formatDuration(hours: Int, minutes: Int, type: UsageLimitType): String {
    val parts = mutableListOf<String>()
    if (hours > 0) parts.add("$hours hour${if (hours != 1) "s" else ""}")
    if (minutes > 0) parts.add("$minutes minute${if (minutes != 1) "s" else ""}")
    
    val duration = parts.joinToString(" ")
    val typeSuffix = when (type) {
        UsageLimitType.DAILY -> "day"
        UsageLimitType.HOURLY -> "hour"
    }
    
    return "$duration / $typeSuffix"
}
