package com.librefocus.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PermissionScreen(
    viewModel: OnboardingViewModel,
    onNext: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val canContinue = viewModel.areNecessaryPermissionsGranted()

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
                PermissionItemRow(item, onPermissionToggled = { granted ->
                    viewModel.updatePermissionStatus(item.permission, granted)
                })
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
