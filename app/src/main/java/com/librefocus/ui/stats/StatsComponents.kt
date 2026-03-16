package com.librefocus.ui.stats

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.librefocus.R
import com.librefocus.models.AppUsageData
import com.librefocus.utils.FormattedDateTimePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
fun StatsTotalAndAverage(
    totalValue: String,
    totalLabel: String,
    averageValue: String,
    averageLabel: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top
    ) {
        // Total column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = totalValue,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = totalLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Average column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = averageValue,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = averageLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatsPeriodNavigator(
    label: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    isNextEnabled: Boolean
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPrevious
        ) {
            Image(
                imageVector = Icons.Outlined.ChevronLeft,
                contentDescription = stringResource(id = R.string.stats_previous_label),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        if (isNextEnabled) {
            IconButton(
                onClick = onNext,
            ) {
                Image(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = stringResource(id = R.string.stats_next_label),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
                )
            }
        }
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
fun PhaseOneInsightsSection(
    insights: PhaseOneInsights,
    selectedMetric: StatsMetric,
    formatted: FormattedDateTimePreferences?
) {
    val comparison = when (selectedMetric) {
        StatsMetric.ScreenTime -> insights.comparison.screenTime
        StatsMetric.Opens -> insights.comparison.opens
    }

    val comparisonMetricLabel = when (selectedMetric) {
        StatsMetric.ScreenTime -> stringResource(id = R.string.stats_metric_screen_time)
        StatsMetric.Opens -> stringResource(id = R.string.stats_metric_opens)
    }

    val currentValueText = when (selectedMetric) {
        StatsMetric.ScreenTime -> formatDuration(comparison.currentValue)
        StatsMetric.Opens -> comparison.currentValue.toString()
    }

    val deltaText = formatDeltaText(comparison, selectedMetric)

    val mostUsedDayLabel = insights.comparison.mostUsedDayUtc?.let { dayUtc ->
        formatted?.formatDate(dayUtc)
    }

    val topHoursText = if (insights.peakHours.topHours.isEmpty()) {
        "-"
    } else {
        insights.peakHours.topHours
            .sorted()
            .joinToString(separator = ", ") { hour ->
                val nextHour = (hour + 1) % 24
                String.format("%02d:00-%02d:00", hour, nextHour)
            }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.stats_insights_header),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        SummaryCard(
            title = stringResource(id = R.string.stats_change_card_title, comparisonMetricLabel),
            value = if (deltaText == null) {
                stringResource(id = R.string.stats_change_no_history, currentValueText)
            } else {
                stringResource(id = R.string.stats_change_with_delta, currentValueText, deltaText)
            }
        )

        SummaryCard(
            title = stringResource(id = R.string.stats_peak_card_title),
            value = stringResource(
                id = R.string.stats_peak_card_value,
                topHoursText,
                insights.peakHours.lateNightPercentage
            )
        )

        SummaryCard(
            title = stringResource(id = R.string.stats_unlock_efficiency_title),
            value = formatUnlockEfficiencyValue(insights.unlockEfficiency)
        )

        SummaryCard(
            title = stringResource(id = R.string.stats_concentration_title),
            value = stringResource(
                id = R.string.stats_concentration_value,
                insights.concentration.top3Percentage,
                insights.concentration.top1Percentage,
                insights.concentration.top5Percentage
            )
        )

        if (mostUsedDayLabel != null) {
            Text(
                text = stringResource(id = R.string.stats_most_used_day, mostUsedDayLabel),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun formatUnlockEfficiencyValue(insight: UnlockEfficiencyInsight): String {
    val minutesPerUnlock = insight.minutesPerUnlock
    val minutesPerLaunch = insight.minutesPerLaunch

    if (minutesPerUnlock == null) {
        return stringResource(id = R.string.stats_unlock_efficiency_no_data)
    }

    val unlockPart = stringResource(
        id = R.string.stats_minutes_per_unlock,
        minutesPerUnlock
    )
    val launchPart = if (minutesPerLaunch != null) {
        stringResource(id = R.string.stats_minutes_per_launch, minutesPerLaunch)
    } else {
        ""
    }
    val checkingPart = if (insight.checkingHeavy) {
        stringResource(id = R.string.stats_checking_heavy_flag)
    } else {
        ""
    }

    return listOf(unlockPart, launchPart, checkingPart)
        .filter { it.isNotBlank() }
        .joinToString(separator = " • ")
}

@Composable
private fun formatDeltaText(comparison: MetricComparison, selectedMetric: StatsMetric): String? {
    val delta = comparison.deltaValue ?: return null
    val deltaPercent = comparison.deltaPercent
    val prefix = if (delta >= 0) "+" else "-"
    val magnitude = delta.absoluteValue

    val deltaValueText = when (selectedMetric) {
        StatsMetric.ScreenTime -> formatDuration(magnitude)
        StatsMetric.Opens -> magnitude.toString()
    }

    return if (deltaPercent == null) {
        "$prefix$deltaValueText"
    } else {
        val pct = deltaPercent.absoluteValue.roundToInt()
        "$prefix$deltaValueText ($prefix$pct%)"
    }
}

@Composable
fun AppUsageListItem(
    appUsage: AppUsageData,
    totalUsageMillis: Long = 0L
) {
    val context = LocalContext.current
    var appIcon by remember { mutableStateOf<Drawable?>(null) }

    LaunchedEffect(appUsage.packageName) {
        appIcon = loadAppIcon(context, appUsage.packageName)
    }

    // Calculate percentage
    val usagePercentage = if (totalUsageMillis > 0L) {
        (appUsage.usageDurationMillis.toFloat() / totalUsageMillis.toFloat())
    } else {
        0f
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            AppIconImage(
                drawable = appIcon,
                contentDescription = appUsage.appName,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // App Info Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = appUsage.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(
                        id = R.string.stats_app_usage_duration,
                        formatDuration(appUsage.usageDurationMillis)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Progress Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { usagePercentage },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${(usagePercentage * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AppIconImage(
    drawable: Drawable?,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    if (drawable != null) {
        val bitmap = remember(drawable) {
            drawable.toBitmap()
        }
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier
        )
    } else {
        // Fallback icon when app icon cannot be loaded
        Icon(
            imageVector = Icons.Outlined.Android,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

suspend fun loadAppIcon(context: Context, packageName: String): Drawable? {
    return withContext(Dispatchers.IO) {
        try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
}
