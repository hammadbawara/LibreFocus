package com.librefocus.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.librefocus.R
import com.librefocus.ui.common.ShowLoading
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val statsContentViewModel: StatsContentViewModel = koinViewModel()
    val period by statsContentViewModel.periodState.collectAsStateWithLifecycle()
    val range by statsContentViewModel.range.collectAsStateWithLifecycle()
    val metric by statsContentViewModel.metric.collectAsStateWithLifecycle()
    val formattedPref by statsContentViewModel.formattedPreferences.collectAsStateWithLifecycle()
    val statsContentUiState by statsContentViewModel.uiState.collectAsStateWithLifecycle()

    val isLoading by remember(uiState.isLoading, statsContentUiState.isLoading) {
        derivedStateOf { uiState.isLoading || statsContentUiState.isLoading }
    }

    LaunchedEffect(period) {
        viewModel.refreshUsageStats(period)
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.stats_title)) },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        ShowLoading (
            isLoading = isLoading,
        ) {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                item {
                    // Display only unlocks in summary - screen time now shown above chart
                    if (uiState.totalUnlocks > 0) {
                        SummaryCard(
                            title = stringResource(id = R.string.stats_total_unlocks_title),
                            value = uiState.totalUnlocks.toString()
                        )
                    }
                }

                item {
                    StatsContent(
                        uiState = statsContentUiState,
                        range = range,
                        metric = metric,
                        period = period,
                        formattedPrefs = formattedPref,
                        onMetricSelected = statsContentViewModel::onMetricSelected,
                        onRangeSelected = statsContentViewModel::onRangeSelected,
                        onNavigateNext = statsContentViewModel::onNavigateNext,
                        onNavigatePrevious = statsContentViewModel::onNavigatePrevious,
                        onCustomRangeSelected = statsContentViewModel::onCustomRangeSelected,
                    )
                }

                item {
                    Text(
                        text = stringResource(id = R.string.stats_top_apps_header),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (uiState.appUsage.isNotEmpty()) {
                    items(
                        items = uiState.appUsage,
                        key = { it.packageName }
                    ) { appUsage ->
                        AppUsageListItem(
                            appUsage = appUsage,
                            totalUsageMillis = statsContentUiState.totalUsageMillis
                        )
                    }
                } else {
                    item {
                        Text(
                            text = stringResource(id = R.string.stats_no_app_usage),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

