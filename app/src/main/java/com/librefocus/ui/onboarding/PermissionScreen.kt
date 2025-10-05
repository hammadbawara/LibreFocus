package com.librefocus.ui.onboarding

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.content.pm.PackageManager
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.core.content.ContextCompat
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

    // Track whether we've performed the initial status check
    var initialCheckDone by remember { mutableStateOf(false) }

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

    // Launcher for Usage Access and other settings flows (we can reuse StartActivityForResult)
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

    // Launcher for battery optimization settings
    val batteryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            val pm = context.getSystemService(PowerManager::class.java)
            val granted = pm?.isIgnoringBatteryOptimizations(context.packageName) == true
            viewModel.updatePermissionStatus(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, granted)
            if (!granted) {
                viewModel.onPermissionDeniedPermanently(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
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

                PermissionEvent.RequestIgnoreBatteryOptimization -> {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:" + context.packageName)
                    }
                    batteryLauncher.launch(intent)
                }
            }
        }
    }

    // Initial permission status check - run once when UIState is available
    LaunchedEffect(uiState.permissions) {
        if (!initialCheckDone && uiState.permissions.isNotEmpty()) {
            uiState.permissions.forEach { item ->
                val granted = when (item.permission) {
                    "android.permission.PACKAGE_USAGE_STATS" -> viewModel.checkUsageAccessGranted(context)
                    Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS -> {
                        val pm = context.getSystemService(PowerManager::class.java)
                        pm?.isIgnoringBatteryOptimizations(context.packageName) == true
                    }
                    else -> {
                        // runtime permission check (covers POST_NOTIFICATIONS on Android 13+ and others)
                        ContextCompat.checkSelfPermission(context, item.permission) == PackageManager.PERMISSION_GRANTED
                    }
                }
                viewModel.updatePermissionStatus(item.permission, granted)
            }
            initialCheckDone = true
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
                    item = item,
                    onRequest = { viewModel.onPermissionRequestClicked(item.permission) }
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
    onRequest: () -> Unit
) {
    val containerColor = if (item.isGranted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onRequest() },
        colors = CardDefaults.cardColors(containerColor = containerColor),
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
                onCheckedChange = {onRequest()},
            )
        }
    }
}
