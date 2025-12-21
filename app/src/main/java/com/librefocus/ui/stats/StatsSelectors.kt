package com.librefocus.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StatsRangeSelector(
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
fun StatsMetricSelector(
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
