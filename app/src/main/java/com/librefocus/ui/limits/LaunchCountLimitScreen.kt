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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.librefocus.models.ResetPeriod
import com.librefocus.ui.common.AppScaffold
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaunchCountLimitScreen(
    onNavigateBack: (LimitConfiguration.LaunchCount?) -> Unit,
    viewModel: LaunchCountLimitViewModel = koinViewModel()
) {
    val maxLaunches by viewModel.maxLaunchesState.collectAsStateWithLifecycle()
    val resetPeriod by viewModel.resetPeriodState.collectAsStateWithLifecycle()
    val selectedDays by viewModel.selectedDaysState.collectAsStateWithLifecycle()
    val isAllWeek by viewModel.isAllWeekState.collectAsStateWithLifecycle()

    AppScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Launch Count Limit") },
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
                    text = "Maximum Launches",
                    style = MaterialTheme.typography.titleSmall
                )

                OutlinedTextField(
                    value = maxLaunches.toString(),
                    onValueChange = { value ->
                        viewModel.setMaxLaunches(value.toIntOrNull() ?: 0)
                    },
                    label = { Text("Number of launches") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Reset Period",
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
                                selected = resetPeriod == ResetPeriod.DAILY,
                                onClick = { viewModel.setResetPeriod(ResetPeriod.DAILY) }
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
                                selected = resetPeriod == ResetPeriod.WEEKLY,
                                onClick = { viewModel.setResetPeriod(ResetPeriod.WEEKLY) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Weekly")
                        }
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
                    val config = viewModel.saveLaunchCountLimit()
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
