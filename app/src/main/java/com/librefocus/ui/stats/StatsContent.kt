package com.librefocus.ui.stats

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.librefocus.utils.FormattedDateTimePreferences
import java.util.concurrent.TimeUnit




@Composable
fun StatsContent(
    uiState: StatsContentUiState,
    period: StatsPeriodState,
    range: StatsRange,
    metric: StatsMetric,
    formattedPrefs: FormattedDateTimePreferences?,
    onMetricSelected: (StatsMetric) -> Unit,
    onRangeSelected: (StatsRange) -> Unit,
    onNavigateNext: () -> Unit,
    onNavigatePrevious: () -> Unit,
    onCustomRangeSelected: (Long, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomRangePicker by rememberSaveable { mutableStateOf(false) }

    if (showCustomRangePicker) {
        CustomRangePickerDialog(
            initialStartDate = period.startUtc,
            initialEndDate = period.endUtc,
            onDismiss = { showCustomRangePicker = false },
            onConfirm = { start, end ->
                onCustomRangeSelected(start, end)
                showCustomRangePicker = false
            }
        )
    }


    Column(
        modifier = modifier
    ) {

        StatsMetricSelector(
            selectedMetric = metric,
            onMetricSelected = onMetricSelected
        )

        StatsRangeSelector(
            selectedRange = range,
            onSelected = { selected ->
                when (selected) {
                    StatsRange.Custom -> {showCustomRangePicker = true}
                    else -> onRangeSelected(selected)
                }
            }
        )

        StatsTotalAndAverage(
            totalValue = uiState.totalDisplayValue,
            totalLabel = uiState.totalDisplayLabel,
            averageValue = uiState.averageDisplayValue,
            averageLabel = uiState.averageDisplayLabel
        )

        UsageChartCard(
            usagePoints = uiState.usagePoints,
            metric = metric,
            range = range,
            formatted = formattedPrefs
        )

        StatsPeriodNavigator(
            label = period.label,
            onPrevious = onNavigatePrevious,
            onNext = onNavigateNext,
            isNextEnabled = true
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomRangePickerDialog(
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
fun rememberCustomDateRangeState(
    initialStartDate: Long,
    initialEndDate: Long
) = androidx.compose.material3.rememberDateRangePickerState(
    initialSelectedStartDateMillis = initialStartDate,
    initialSelectedEndDateMillis = initialEndDate - TimeUnit.DAYS.toMillis(1)
)