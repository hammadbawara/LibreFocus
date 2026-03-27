package com.librefocus.ui.limits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.librefocus.models.Limit
import com.librefocus.ui.common.AppBottomNavigationBar
import com.librefocus.ui.common.AppScaffold
import com.librefocus.utils.PermissionUtils
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LimitsScreen(
    navController: NavController,
    currentRoute: String?,
    modifier: Modifier = Modifier,
    viewModel: LimitsViewModel = koinViewModel()
) {
    val limits by viewModel.limitsState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var hasPermissions by remember { mutableStateOf(PermissionUtils.isAccessibilityServiceEnabled(context) && PermissionUtils.canDrawOverlays(context)) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasPermissions = PermissionUtils.isAccessibilityServiceEnabled(context) && PermissionUtils.canDrawOverlays(context)
    }

    AppScaffold(
        modifier = modifier,
        topBar = { scrollBehavior->
            LargeTopAppBar(
                title = { Text("Limits") },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = { scrollBehavior ->
            AppBottomNavigationBar(
                navController = navController,
                currentRoute = currentRoute,
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("create_limit") }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create limit")
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        if (limits.isEmpty()) {
            EmptyLimitsState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (!hasPermissions) {
                    PermissionWarningCard(
                        onClick = { showPermissionDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(limits, key = { it.id }) { limit ->
                        LimitCard(
                            limit = limit,
                            onToggle = { viewModel.toggleLimitEnabled(limit.id, it) },
                            onClick = { navController.navigate("create_limit/${limit.id}") }
                        )
                    }
                }
            }
        }
    }

    if (showPermissionDialog) {
        LimitsPermissionDialog(
            onDismissRequest = { showPermissionDialog = false },
            onPermissionsGranted = {
                showPermissionDialog = false
                hasPermissions = true
            }
        )
    }
}

@Composable
private fun EmptyLimitsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Block,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No limits created yet",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PermissionWarningCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Limits are not working",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "App does not have proper permissions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            TextButton(
                onClick = onClick,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text("Fix it")
            }
        }
    }
}

@Composable
private fun LimitCard(
    limit: Limit,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = limit.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = getLimitSummary(limit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (limit.selectedAppPackages.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AppIconsRow(
                        packages = limit.selectedAppPackages,
                        maxVisible = 5
                    )
                }
            }
            Switch(
                checked = limit.isEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
private fun AppIconsRow(
    packages: List<String>,
    maxVisible: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        packages.take(maxVisible).forEach { _ ->
            Icon(
                imageVector = Icons.Default.Block,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        if (packages.size > maxVisible) {
            Text(
                text = "+${packages.size - maxVisible} more",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getLimitSummary(limit: Limit): String {
    return when (limit) {
        is Limit.Schedule -> LimitSummaryFormatter.formatScheduleSummary(
            isAllDay = limit.isAllDay,
            timeSlots = limit.timeSlots,
            selectedDays = limit.selectedDays
        )
        is Limit.UsageLimit -> LimitSummaryFormatter.formatUsageSummary(
            limitType = limit.limitType,
            durationMinutes = limit.durationMinutes,
            selectedDays = limit.selectedDays
        )
        is Limit.LaunchCount -> LimitSummaryFormatter.formatLaunchCountSummary(
            maxLaunches = limit.maxLaunches,
            resetPeriod = limit.resetPeriod,
            selectedDays = limit.selectedDays
        )
    }
}