package com.librefocus.ui.limits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.librefocus.models.UsageLimitType
import com.librefocus.ui.common.AppScaffold
import com.librefocus.ui.common.AppTopAppBar
import com.librefocus.ui.common.PrimaryActionButton
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageLimitScreen(
    onNavigateBack: (LimitConfiguration.Usage?) -> Unit,
    viewModel: UsageLimitViewModel = koinViewModel()
) {
    val limitType by viewModel.limitTypeState.collectAsStateWithLifecycle()
    val minutesInput by viewModel.minutesInputState.collectAsStateWithLifecycle()
    val selectedDays by viewModel.selectedDaysState.collectAsStateWithLifecycle()
    val isAllWeek by viewModel.isAllWeekState.collectAsStateWithLifecycle()
    val isSaveEnabled by viewModel.isSaveEnabled.collectAsStateWithLifecycle()
    val validationError by viewModel.validationErrorState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(validationError) {
        validationError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearValidationError()
        }
    }

    AppScaffold(
        topBar = { scrollBehavior ->
            AppTopAppBar(
                title = { Text("Usage Limit") },
                showNavigationIcon = true,
                onClickNavigationIcon = { onNavigateBack(null) },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
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
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = limitType == UsageLimitType.DAILY,
                        onClick = { viewModel.setLimitType(UsageLimitType.DAILY) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Daily")
                    }
                    SegmentedButton(
                        selected = limitType == UsageLimitType.HOURLY,
                        onClick = { viewModel.setLimitType(UsageLimitType.HOURLY) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Hourly")
                    }
                }

                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (limitType == UsageLimitType.DAILY) "Daily Time Limit" else "Hourly Time Limit",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        OutlinedTextField(
                            value = minutesInput,
                            onValueChange = { viewModel.setMinutesInput(it) },
                            label = { 
                                Text(
                                    if (limitType == UsageLimitType.DAILY) 
                                        "Minutes per day" 
                                    else 
                                        "Minutes per hour (0-59)"
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            supportingText = {
                                Text(
                                    if (limitType == UsageLimitType.DAILY)
                                        "Total minutes allowed per day"
                                    else
                                        "Minutes allowed in each hour (max 59)"
                                )
                            }
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

            PrimaryActionButton(
                onClick = {
                    val config = viewModel.validateAndSave()
                    if (config != null) {
                        onNavigateBack(config)
                    }
                },
                enabled = isSaveEnabled,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Save")
            }
        }
    }
}
