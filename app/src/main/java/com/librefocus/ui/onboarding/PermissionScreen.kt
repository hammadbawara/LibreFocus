package com.librefocus.ui.onboarding

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest

@Composable
fun PermissionScreen(
    viewModel: OnboardingViewModel,
    onNext: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val canContinue = viewModel.areNecessaryPermissionsGranted()
    val context = LocalContext.current
    val activity = context as? Activity

    // State to keep track of last requested permission
    var lastRequestedPermission by remember { mutableStateOf<String?>(null) }

    // Permission launcher for standard permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            val lastPermission = lastRequestedPermission
            if (lastPermission != null) {
                viewModel.updatePermissionStatus(lastPermission, granted)
                if (!granted) {
                    val showRationale = activity?.shouldShowRequestPermissionRationale(lastPermission) ?: false
                    if (!showRationale) {
                        viewModel.onPermissionDeniedPermanently(lastPermission)
                    }
                }
            }
        }
    )
    // Launcher for Usage Access
    val usageAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            val granted = viewModel.checkUsageAccessGranted(context)
            viewModel.updatePermissionStatus("android.permission.PACKAGE_USAGE_STATS", granted)
            if (!granted) {
                viewModel.onPermissionDeniedPermanently("android.permission.PACKAGE_USAGE_STATS")
            }
        }
    )

    // Launcher for App Info
    val appInfoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {}
    )

    // Collect permission events from ViewModel
    LaunchedEffect(Unit) {
        viewModel.permissionEvents.collectLatest { event ->
            when (event) {
                is PermissionEvent.RequestPermission -> {
                    lastRequestedPermission = event.permission
                    permissionLauncher.launch(event.permission)
                }
                is PermissionEvent.RequestUsageAccess -> {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    usageAccessLauncher.launch(intent)
                }
                is PermissionEvent.OpenAppInfo -> {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    appInfoLauncher.launch(intent)
                }

                PermissionEvent.RequestIgnoreBatteryOptimization -> TODO()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            "App Permissions",
            fontWeight = FontWeight.Bold,
            fontSize = MaterialTheme.typography.headlineSmall.fontSize
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "To help you focus effectively, LibreFocus needs the following permissions:",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(uiState.permissions) { item ->
                PermissionItemRow(
                    item,
                    onPermissionToggled = { granted ->
                        if (granted) {
                            viewModel.onPermissionRequestClicked(item.permission)
                        } else {
                            // If user unchecks, mark as not granted
                            viewModel.updatePermissionStatus(item.permission, false)
                        }
                    }
                )
            }
        }

        Button(
            onClick = onNext,
            enabled = canContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Next")
        }
    }
}

@Composable
private fun PermissionItemRow(
    item: PermissionItem,
    onPermissionToggled: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = item.iconRes),
                contentDescription = item.name,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Bold)
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Checkbox(
                checked = item.isGranted,
                onCheckedChange = { onPermissionToggled(it) }
            )
        }
    }
}
