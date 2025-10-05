package com.librefocus.ui.home

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import com.librefocus.models.AppUsage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Today’s Usage") })
        }
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding)) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
            state.error != null -> Text(
                text = "Error: ${state.error}",
                modifier = Modifier.padding(16.dp)
            )
            else -> AppUsageList(
                apps = state.apps,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
fun AppUsageList(apps: List<AppUsage>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(apps) { app ->
            AppUsageItem(app)
            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
        }
    }
}

@Composable
fun AppUsageItem(app: AppUsage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row {
            val icon = app.icon as? Drawable
            icon?.let {
                Image(
                    bitmap = it.toBitmap().asImageBitmap(),
                    contentDescription = app.appName,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = app.appName, style = MaterialTheme.typography.bodyLarge)
        }

        Text(
            text = formatUsageTime(app.usageTimeMillis),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun formatUsageTime(millis: Long): String {
    val minutes = (millis / 1000) / 60
    val hours = minutes / 60
    return if (hours > 0) "%dh %dm".format(hours, minutes % 60) else "%dm".format(minutes)
}
