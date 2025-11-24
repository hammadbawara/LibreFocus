package com.librefocus.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.librefocus.R
import com.librefocus.models.AppUsageData

@Composable
fun StatsPeriodNavigator(
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
fun RowControls(
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    isNextEnabled: Boolean
) {
    Row(
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
fun StatsSummarySection(uiState: StatsUiState) {
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
fun SummaryCard(
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
fun AppUsageListItem(appUsage: AppUsageData) {
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
