package com.librefocus.ui.limits

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.librefocus.models.ResetPeriod
import com.librefocus.ui.common.AppScaffold
import com.librefocus.ui.common.PrimaryActionButton
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
    val isSaveEnabled by viewModel.isSaveEnabled.collectAsStateWithLifecycle()
    val validationError by viewModel.validationErrorState.collectAsStateWithLifecycle()

    var showEditDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(validationError) {
        validationError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearValidationError()
        }
    }

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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = resetPeriod == ResetPeriod.DAILY,
                        onClick = { viewModel.setResetPeriod(ResetPeriod.DAILY) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Daily")
                    }
                    SegmentedButton(
                        selected = resetPeriod == ResetPeriod.WEEKLY,
                        onClick = { viewModel.setResetPeriod(ResetPeriod.WEEKLY) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Weekly")
                    }
                }

                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Maximum Launches",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedIconButton(
                                onClick = { viewModel.decrementLaunches() },
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Decrease",
                                    modifier = Modifier.graphicsLayer { rotationZ = 180f })
                            }

                            OutlinedCard(
                                modifier = Modifier
                                    .padding(horizontal = 24.dp)
                                    .clickable { showEditDialog = true },
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Text(
                                    text = maxLaunches.toString(),
                                    style = MaterialTheme.typography.displayLarge,
                                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
                                    textAlign = TextAlign.Center
                                )
                            }

                            OutlinedIconButton(
                                onClick = { viewModel.incrementLaunches() },
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase")
                            }
                        }

                        Text(
                            text = "Tap number to edit manually",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

    if (showEditDialog) {
        EditLaunchCountDialog(
            currentValue = maxLaunches,
            onDismiss = { showEditDialog = false },
            onConfirm = { newValue ->
                viewModel.setMaxLaunches(newValue)
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun EditLaunchCountDialog(
    currentValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var inputValue by remember { mutableStateOf(currentValue.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Launch Count") },
        text = {
            OutlinedTextField(
                value = inputValue,
                onValueChange = { inputValue = it },
                label = { Text("Number of launches") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = inputValue.toIntOrNull()
                    if (value != null && value > 0) {
                        onConfirm(value)
                    }
                }
            ) {
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
