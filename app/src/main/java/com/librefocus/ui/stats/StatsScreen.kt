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
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.librefocus.R
import com.librefocus.ui.common.AppBottomNavigationBar
import com.librefocus.ui.common.AppScaffold
import com.librefocus.ui.common.ShowLoading
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    navController: NavController,
    currentRoute: String?,
    viewModel: StatsViewModel = koinViewModel(),
    onAppClick: (packageName: String, appName: String) -> Unit = { _, _ -> }
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

    
    AppScaffold(
        topBar = { scrollBehavior ->
            LargeTopAppBar(
                title = { Text(text = stringResource(id = R.string.stats_title)) },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = { scrollBehavior ->
            AppBottomNavigationBar(
                navController = navController,
                currentRoute = currentRoute,
                scrollBehavior = scrollBehavior
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
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                item {
                    StatsMetricSelector(
                        selectedMetric = metric,
                        onMetricSelected = viewModel::onMetricSelected
                    )
                }

                item {
                    StatsRangeSelector(
                        selectedRange = range,
                        onSelected = { selected ->
                            when (selected) {
                                StatsRange.Custom -> showCustomRangePicker = true
                                else -> viewModel.onRangeSelected(selected)
                            }
                        }
                    )
                }

                item {
                    // Consume pre-calculated display values from ViewModel state
                    StatsTotalAndAverage(
                        totalValue = uiState.totalDisplayValue,
                        totalLabel = uiState.totalDisplayLabel,
                        averageValue = uiState.averageDisplayValue,
                        averageLabel = uiState.averageDisplayLabel
                    )
                }

                item {
                    StatsPeriodNavigator(
                        label = period.label,
                        onPrevious = viewModel::onNavigatePrevious,
                        onNext = viewModel::onNavigateNext,
                        isNextEnabled = true
                    )
                }

                item {
                    UsageChartCard(
                        usagePoints = uiState.usagePoints,
                        metric = metric,
                        range = range,
                        formatted = formattedPrefs
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                item {
                    uiState.phaseOneInsights?.let { insights ->
                        PhaseOneInsightsSection(
                            insights = insights,
                            selectedMetric = metric,
                            formatted = formattedPrefs
                        )
                    }
                }

                item {
                    uiState.phaseTwoInsights?.let { insights ->
                        PhaseTwoInsightsSection(insights = insights)
                    }
                }

                item {
                    uiState.phaseThreeInsights?.let { insights ->
                        PhaseThreeInsightsSection(insights = insights, range = range)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
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
                            totalUsageMillis = statsContentUiState.totalUsageMillis,
                            onClick = { onAppClick(appUsage.packageName, appUsage.appName) }
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

