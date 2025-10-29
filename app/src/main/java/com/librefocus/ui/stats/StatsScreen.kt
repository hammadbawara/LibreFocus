package com.librefocus.ui.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.librefocus.R
import com.librefocus.models.AppUsageData
import com.librefocus.models.UsageValuePoint
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val range by viewModel.range.collectAsStateWithLifecycle()
    val metric by viewModel.metric.collectAsStateWithLifecycle()
    val period by viewModel.periodState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    var showCustomRangePicker by rememberSaveable { mutableStateOf(false) }

    if (showCustomRangePicker) {
        CustomRangePickerDialog(
            initialStartDate = period.currentStartUtc,
            initialEndDate = period.currentEndUtc,
            onDismiss = { showCustomRangePicker = false },
            onConfirm = { start, end ->
                viewModel.onCustomRangeSelected(start, end)
                showCustomRangePicker = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.stats_title)) }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    StatsPeriodNavigator(
                        label = period.label,
                        onPrevious = viewModel::onNavigatePrevious,
                        onNext = viewModel::onNavigateNext,
                        isNextEnabled = true
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
                    StatsMetricSelector(
                        selectedMetric = metric,
                        onMetricSelected = viewModel::onMetricSelected
                    )
                }

                item {
                    UsageChartCard(
                        usagePoints = uiState.usagePoints,
                        metric = metric,
                        range = range
                    )
                }

                item {
                    StatsSummarySection(uiState)
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
                        AppUsageListItem(appUsage)
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

            AnimatedVisibility(
                visible = uiState.isLoading,
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp)
                )
            }
        }
    }
}

@Composable
private fun StatsPeriodNavigator(
    label: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    isNextEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            RowControls(onPrevious = onPrevious, onNext = onNext, isNextEnabled = isNextEnabled)
        }
    }
}

@Composable
private fun RowControls(
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    isNextEnabled: Boolean
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        AssistChip(
            onClick = onPrevious,
            label = { Text(text = stringResource(id = R.string.stats_previous_label)) },
            leadingIcon = {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Outlined.ChevronLeft,
                    contentDescription = stringResource(id = R.string.stats_previous_label)
                )
            }
        )
        AssistChip(
            onClick = onNext,
            enabled = isNextEnabled,
            colors = AssistChipDefaults.assistChipColors(
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            label = { Text(text = stringResource(id = R.string.stats_next_label)) },
            leadingIcon = {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = stringResource(id = R.string.stats_next_label)
                )
            }
        )
    }
}

@Composable
private fun StatsRangeSelector(
    selectedRange: StatsRange,
    onSelected: (StatsRange) -> Unit
) {
    val ranges = StatsRange.entries
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(ranges) { range ->
            AssistChip(
                onClick = { onSelected(range) },
                label = { Text(text = range.displayName()) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (selectedRange == range) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    labelColor = if (selectedRange == range) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                ),
                leadingIcon = when (range) {
                    StatsRange.Day -> {
                        { androidx.compose.material3.Icon(imageVector = Icons.Outlined.Leaderboard, contentDescription = range.displayName()) }
                    }
                    StatsRange.Week -> null
                    StatsRange.Month -> null
                    StatsRange.Custom -> null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatsMetricSelector(
    selectedMetric: StatsMetric,
    onMetricSelected: (StatsMetric) -> Unit
) {
    val metrics = StatsMetric.entries
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        metrics.forEachIndexed { index, metric ->
            val shape = SegmentedButtonDefaults.itemShape(index = index, count = metrics.size)
            SegmentedButton(
                selected = metric == selectedMetric,
                onClick = { onMetricSelected(metric) },
                shape = shape,
                label = { Text(text = metric.displayName()) }
            )
        }
    }
}

@Composable
private fun UsageChartCard(
    usagePoints: List<UsageValuePoint>,
    metric: StatsMetric,
    range: StatsRange
) {
    val chartModelProducer = remember { CartesianChartModelProducer() }
    val yValues = remember(usagePoints, metric) {
        usagePoints.map { point ->
            when (metric) {
                StatsMetric.ScreenTime -> point.totalUsageMillis.toDouble() / TimeUnit.MINUTES.toMillis(1).toDouble()
                StatsMetric.Opens -> point.totalLaunchCount.toDouble()
            }
        }
    }
    val bottomLabels = remember(usagePoints, range) {
        usagePoints.map { point ->
            formatBottomLabel(point, range)
        }
    }

    LaunchedEffect(yValues) {
        chartModelProducer.runTransaction {
            lineSeries {
                val values = if (yValues.isEmpty()) listOf(0.0) else yValues
                series(values)
            }
        }
    }

    val startAxisFormatter = remember(metric) {
        CartesianValueFormatter { _, value, _ ->
            when (metric) {
                StatsMetric.ScreenTime -> formatMinutesLabel(value)
                StatsMetric.Opens -> value.roundToInt().toString()
            }
        }
    }
    val bottomAxisFormatter = remember(bottomLabels) {
        CartesianValueFormatter { _, value, _ ->
            bottomLabels.getOrNull(value.roundToInt()) ?: ""
        }
    }

    val lineLayer = rememberLineCartesianLayer()
    val chart = rememberCartesianChart(
        lineLayer,
        startAxis = VerticalAxis.rememberStart(valueFormatter = startAxisFormatter),
        bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = bottomAxisFormatter)
    )
    val vicoTheme = rememberM3VicoTheme()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = metric.displayName(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            ProvideVicoTheme(theme = vicoTheme) {
                CartesianChartHost(
                    chart = chart,
                    modelProducer = chartModelProducer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            }
        }
    }
}

@Composable
private fun StatsSummarySection(uiState: StatsUiState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryCard(
            title = stringResource(id = R.string.stats_total_usage_title),
            value = formatDuration(uiState.totalUsageMillis)
        )
        SummaryCard(
            title = stringResource(id = R.string.stats_average_session_title),
            value = formatDuration(uiState.averageSessionMillis)
        )
        SummaryCard(
            title = stringResource(id = R.string.stats_total_unlocks_title),
            value = uiState.totalUnlocks.toString()
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AppUsageListItem(appUsage: AppUsageData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = appUsage.appName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    id = R.string.stats_app_usage_duration,
                    formatDuration(appUsage.usageDurationMillis)
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(
                    id = R.string.stats_app_usage_launches,
                    appUsage.launchCount
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomRangePickerDialog(
    initialStartDate: Long,
    initialEndDate: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long) -> Unit
) {
    val pickerState = rememberCustomDateRangeState(initialStartDate, initialEndDate)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val start = pickerState.selectedStartDateMillis
                    val end = pickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        onConfirm(start, end)
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        }
    ) {
        DateRangePicker(state = pickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberCustomDateRangeState(
    initialStartDate: Long,
    initialEndDate: Long
) = androidx.compose.material3.rememberDateRangePickerState(
    initialSelectedStartDateMillis = initialStartDate,
    initialSelectedEndDateMillis = initialEndDate - TimeUnit.DAYS.toMillis(1)
)

private fun StatsRange.displayName(): String {
    return when (this) {
        StatsRange.Day -> "Day"
        StatsRange.Week -> "Week"
        StatsRange.Month -> "Month"
        StatsRange.Custom -> "Custom"
    }
}

private fun StatsMetric.displayName(): String {
    return when (this) {
        StatsMetric.ScreenTime -> "Screen time"
        StatsMetric.Opens -> "Opens"
    }
}

private fun formatDuration(millis: Long): String {
    if (millis <= 0L) return "0m"
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    return buildString {
        if (hours > 0) {
            append(hours)
            append('h')
            if (minutes > 0) append(' ')
        }
        if (minutes > 0 || hours == 0L) {
            append(minutes)
            append('m')
        }
    }
}

private fun formatBottomLabel(point: UsageValuePoint, range: StatsRange): String {
    val instant = Instant.ofEpochMilli(point.bucketStartUtc)
    val zone = ZoneId.systemDefault()
    return when (range) {
        StatsRange.Day -> hourFormatter.format(instant.atZone(zone))
        StatsRange.Week, StatsRange.Month, StatsRange.Custom -> dayFormatter.format(instant.atZone(zone))
    }
}

private fun formatMinutesLabel(value: Double): String {
    val minutes = value.roundToInt()
    return if (minutes < 60) {
        "${minutes}m"
    } else {
        val hours = minutes / 60
        val remaining = minutes % 60
        if (remaining == 0) {
            "${hours}h"
        } else {
            "${hours}h ${remaining}m"
        }
    }
}

private val hourFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH").withLocale(Locale.getDefault())

private val dayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd MMM").withLocale(Locale.getDefault())
