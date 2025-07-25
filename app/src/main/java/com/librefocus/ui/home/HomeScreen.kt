package com.librefocus.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.librefocus.ui.common.AppBottomNavigationBar
import com.librefocus.ui.common.AppScaffold
import com.librefocus.ui.home.components.AnalyticsSection
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    currentRoute: String?,
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AppScaffold(
        topBar = { scrollBehavior->
            LargeTopAppBar(
                title = { Text("Today's Usage") },
                actions = {
                    IconButton(
                        onClick = { viewModel.syncUsageStats() },
                        enabled = !state.isSyncing
                    ) {
                        if (state.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = com.librefocus.R.drawable.baseline_sync_24),
                                contentDescription = "Sync Usage Data"
                            )
                        }
                    }
                },
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
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                AnalyticsSection(
                    heatmapData = state.heatmapData,
                    insights = state.insights
                )
            }

//            if (state.apps.isNotEmpty()) {
//                item {
//                    Text(
//                        text = "Today's Apps",
//                        style = MaterialTheme.typography.titleMedium,
//                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
//                    )
//                }
//
//                items(state.apps) { appUsage ->
//                    ListItem(
//                        headlineContent = { Text(appUsage.appName) },
//                        supportingContent = {
//                            Text(
//                                "Usage: ${formatHomeDuration(appUsage.usageTimeMillis)}"
//                            )
//                        }
//                    )
//                }
//            }
        }
    }
}

private fun formatHomeDuration(millis: Long): String {
    val totalMinutes = millis / (1000 * 60)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m"
    }
}
