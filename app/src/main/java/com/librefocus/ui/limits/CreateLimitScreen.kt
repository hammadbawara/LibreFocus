package com.librefocus.ui.limits

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.librefocus.ui.appselection.AppSelectionBottomSheet
import com.librefocus.ui.common.AppScaffold
import com.librefocus.ui.common.PrimaryActionButton
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateLimitScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSetLimit: () -> Unit,
    limitConfigResult: LimitConfiguration? = null,
    viewModel: CreateLimitViewModel = koinViewModel()
) {
    // Update configuration when received from navigation
    limitConfigResult?.let {
        viewModel.setLimitConfiguration(it)
    }
    
    val limitName by viewModel.limitNameState.collectAsStateWithLifecycle()
    val selectedApps by viewModel.selectedAppsState.collectAsStateWithLifecycle()
    val limitConfig by viewModel.limitConfigurationState.collectAsStateWithLifecycle()
    val validationErrors by viewModel.validationErrorsState.collectAsStateWithLifecycle()
    val hasChanges by viewModel.hasChangesState.collectAsStateWithLifecycle()
    val isEditMode by viewModel.isEditMode.collectAsStateWithLifecycle()

    var showDiscardDialog by remember { mutableStateOf(false) }
    var showAppSelection by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    BackHandler {
        if (hasChanges) {
            showDiscardDialog = true
        } else {
            onNavigateBack()
        }
    }

    AppScaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Limit" else "Create Limit") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasChanges) {
                            showDiscardDialog = true
                        } else {
                            onNavigateBack()
                        }
                    }) {
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = limitName,
                onValueChange = {
                    viewModel.setLimitName(it)
                    viewModel.clearValidationErrors()
                },
                label = { Text("Limit Name") },
                modifier = Modifier.fillMaxWidth(),
                isError = validationErrors.nameError,
                supportingText = if (validationErrors.nameError) {
                    { Text("Limit name is required", color = MaterialTheme.colorScheme.error) }
                } else null
            )

            SetLimitSection(
                limitConfig = limitConfig,
                isError = validationErrors.limitTypeError,
                onClick = onNavigateToSetLimit
            )

            SelectAppsSection(
                selectedApps = selectedApps,
                isError = validationErrors.appsError,
                onClick = { showAppSelection = true }
            )

            Spacer(modifier = Modifier.weight(1f))

            PrimaryActionButton(
                onClick = {
                    val success = viewModel.validateAndSave()
                    if (success) {
                        onNavigateBack()
                    } else {
                        coroutineScope.launch {
                            val errorMessage = when {
                                validationErrors.nameError -> "Please enter a limit name"
                                validationErrors.limitTypeError -> "Please set a limit"
                                validationErrors.appsError -> "Please select at least one app"
                                else -> "Please fix all errors"
                            }
                            snackbarHostState.showSnackbar(errorMessage)
                        }
                    }
                }
            ) {
                Text(if (isEditMode) "Update Limit" else "Create Limit")
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("Your unsaved changes will be lost") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAppSelection) {
        AppSelectionBottomSheet(
            onDismiss = { showAppSelection = false },
            onConfirm = { packages ->
                viewModel.setSelectedApps(packages)
                viewModel.clearValidationErrors()
                showAppSelection = false
            },
            preSelectedPackages = selectedApps.toSet()
        )
    }
}

@Composable
private fun SetLimitSection(
    limitConfig: LimitConfiguration?,
    isError: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = if (isError) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            )
        } else CardDefaults.cardColors()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Set Limit",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (limitConfig) {
                        is LimitConfiguration.Schedule -> LimitSummaryFormatter.formatScheduleSummary(
                            limitConfig.isAllDay,
                            limitConfig.timeSlots,
                            limitConfig.selectedDays
                        )
                        is LimitConfiguration.Usage -> LimitSummaryFormatter.formatUsageSummary(
                            limitConfig.limitType,
                            limitConfig.durationMinutes,
                            limitConfig.selectedDays
                        )
                        is LimitConfiguration.LaunchCount -> LimitSummaryFormatter.formatLaunchCountSummary(
                            limitConfig.maxLaunches,
                            limitConfig.resetPeriod,
                            limitConfig.selectedDays
                        )
                        null -> "Tap to configure"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (limitConfig == null) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null
            )
        }
    }
}

@Composable
private fun SelectAppsSection(
    selectedApps: List<String>,
    isError: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = if (isError) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            )
        } else CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Apps",
                    style = MaterialTheme.typography.titleSmall
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedApps.size.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null
                    )
                }
            }

            if (selectedApps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${selectedApps.size} app${if (selectedApps.size != 1) "s" else ""} selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No apps selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}
