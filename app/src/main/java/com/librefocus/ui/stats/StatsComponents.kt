package com.librefocus.ui.stats

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.librefocus.R
import com.librefocus.models.AppUsageData
import com.librefocus.utils.FormattedDateTimePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// Basic reusable card
// ─────────────────────────────────────────────────────────────────────────────

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
        IconButton(onClick = onPrevious) {
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
            IconButton(onClick = onNext) {
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
fun SummaryCard(
    title: String,
    value: String
) {
    InsightCard {
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

/** Base card with consistent styling used by all insight cards. */
@Composable
private fun InsightCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            content()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Phase 1 Insights
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PhaseOneInsightsSection(
    insights: PhaseOneInsights,
    selectedMetric: StatsMetric,
    formatted: FormattedDateTimePreferences?
) {
    val comparison = when (selectedMetric) {
        StatsMetric.ScreenTime -> insights.comparison.screenTime
        StatsMetric.Opens -> insights.comparison.opens
        StatsMetric.Unlocks -> insights.comparison.unlocks
    }
    val comparisonMetricLabel = when (selectedMetric) {
        StatsMetric.ScreenTime -> stringResource(id = R.string.stats_metric_screen_time)
        StatsMetric.Opens -> stringResource(id = R.string.stats_metric_opens)
        StatsMetric.Unlocks -> stringResource(id = R.string.stats_metric_unlocks)
    }
    val mostUsedDayLabel = insights.comparison.mostUsedDayUtc?.let { formatted?.formatDate(it) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.stats_insights_header),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        ComparisonInsightCard(
            comparison = comparison,
            metric = selectedMetric,
            metricLabel = comparisonMetricLabel,
            mostUsedDayLabel = mostUsedDayLabel
        )

        PeakHoursInsightCard(insight = insights.peakHours)

        UnlockEfficiencyInsightCard(insight = insights.unlockEfficiency)

        ConcentrationInsightCard(insight = insights.concentration)
    }
}

/** Shows current vs previous period with a colour-coded delta badge. */
@Composable
private fun ComparisonInsightCard(
    comparison: MetricComparison,
    metric: StatsMetric,
    metricLabel: String,
    mostUsedDayLabel: String?
) {
    val currentValueText = when (metric) {
        StatsMetric.ScreenTime -> formatDuration(comparison.currentValue)
        StatsMetric.Opens, StatsMetric.Unlocks -> comparison.currentValue.toString()
    }
    val previousText = comparison.previousValue?.let {
        when (metric) {
            StatsMetric.ScreenTime -> "prev: ${formatDuration(it)}"
            StatsMetric.Opens, StatsMetric.Unlocks -> "prev: $it"
        }
    }

    // Delta chip colours: for screen time, negative delta = GOOD (less usage)
    val delta = comparison.deltaValue
    val deltaPercent = comparison.deltaPercent
    val (deltaChipColor, deltaTextColor, _, deltaText) = if (delta != null) {
        val improved = when (metric) {
            StatsMetric.ScreenTime -> delta < 0
            StatsMetric.Opens, StatsMetric.Unlocks -> delta < 0
        }
        val chipBg = if (improved) Color(0xFF1E6B3A) else Color(0xFF8B1A1A)
        val chipText = Color.White
        val prefix = if (delta >= 0) "+" else "−"
        val abs = delta.absoluteValue
        val valueStr = when (metric) {
            StatsMetric.ScreenTime -> formatDuration(abs)
            StatsMetric.Opens, StatsMetric.Unlocks -> abs.toString()
        }
        val pct = deltaPercent?.let { " (${it.absoluteValue.roundToInt()}%)" } ?: ""
        Quadruple(chipBg, chipText, prefix, "$prefix$valueStr$pct")
    } else {
        Quadruple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "",
            "No prior data"
        )
    }

    InsightCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(id = R.string.stats_change_card_title, metricLabel),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentValueText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                previousText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Delta badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(deltaChipColor)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = deltaText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = deltaTextColor
                )
            }
        }
        mostUsedDayLabel?.let { day ->
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(id = R.string.stats_most_used_day, day),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Peak hours shown as pill chips; late-night % as a coloured progress bar. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PeakHoursInsightCard(insight: PeakHoursInsight) {
    InsightCard {
        Text(
            text = stringResource(id = R.string.stats_peak_card_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (insight.topHours.isEmpty()) {
            Text(
                text = "–",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                insight.topHours.sorted().forEach { hour ->
                    val nextHour = (hour + 1) % 24
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                text = String.format("%02d:00–%02d:00", hour, nextHour),
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        border = null
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val lateNightFraction = insight.lateNightPercentage / 100f
        val lateNightColor = when {
            insight.lateNightPercentage >= 40 -> Color(0xFF8B1A1A)
            insight.lateNightPercentage >= 20 -> Color(0xFFC47A00)
            else -> MaterialTheme.colorScheme.primary
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Late-night usage",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(100.dp)
            )
            val animatedFraction by animateFloatAsState(
                targetValue = lateNightFraction,
                animationSpec = tween(600),
                label = "lateNightBar"
            )
            LinearProgressIndicator(
                progress = { animatedFraction },
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = lateNightColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
            Text(
                text = "${insight.lateNightPercentage}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = lateNightColor
            )
        }
    }
}

/** Unlock efficiency with large number and optional "checking heavy" warning badge. */
@Composable
private fun UnlockEfficiencyInsightCard(insight: UnlockEfficiencyInsight) {
    InsightCard {
        Text(
            text = stringResource(id = R.string.stats_unlock_efficiency_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (insight.minutesPerUnlock == null) {
            Text(
                text = stringResource(id = R.string.stats_unlock_efficiency_no_data),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = String.format("%.1f", insight.minutesPerUnlock),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "min/unlock",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    insight.minutesPerLaunch?.let { mpl ->
                        Text(
                            text = String.format("%.1f min/open", mpl),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (insight.checkingHeavy) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF8B1A1A).copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = Color(0xFF8B1A1A),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.stats_checking_heavy_flag),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF8B1A1A)
                        )
                    }
                }
            }
        }
    }
}

/** Concentration: top 1/3/5 apps as stacked rows with animated progress bars. */
@Composable
private fun ConcentrationInsightCard(insight: ConcentrationInsight) {
    InsightCard {
        Text(
            text = stringResource(id = R.string.stats_concentration_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))

        listOf(
            Triple("Top 1 app", insight.top1Percentage, MaterialTheme.colorScheme.primary),
            Triple("Top 3 apps", insight.top3Percentage, MaterialTheme.colorScheme.secondary),
            Triple("Top 5 apps", insight.top5Percentage, MaterialTheme.colorScheme.tertiary)
        ).forEach { (label, percent, color) ->
            val fraction = percent / 100f
            val animatedFraction by animateFloatAsState(
                targetValue = fraction,
                animationSpec = tween(600),
                label = "concentration_$label"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(64.dp)
                )
                LinearProgressIndicator(
                    progress = { animatedFraction },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.width(36.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Phase 2 Insights
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PhaseTwoInsightsSection(insights: PhaseTwoInsights) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.stats_phase_two_header),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        HeatmapInsightCard(
            cells = insights.heatmap.cells,
            peakWeekday = insights.heatmap.peakWeekday,
            peakHour = insights.heatmap.peakHour,
            windowDays = insights.heatmap.windowDays
        )

        StreaksInsightCard(insight = insights.streaks)

        AppSprawlInsightCard(insight = insights.appSprawl)
    }
}

@Composable
private fun HeatmapInsightCard(
    cells: List<HeatmapCell>,
    peakWeekday: Int?,
    peakHour: Int?,
    windowDays: Int
) {
    val grouped = cells.groupBy { it.weekday }
    val scrollState = rememberScrollState()

    InsightCard {
        Text(
            text = stringResource(id = R.string.stats_phase_two_heatmap_title),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        // Peak summary text
        val peakSummary = if (peakWeekday != null && peakHour != null) {
            val dayName = DayOfWeek.of(peakWeekday).getDisplayName(TextStyle.SHORT, Locale.getDefault())
            val end = (peakHour + 1) % 24
            "Busiest: $dayName ${String.format("%02d:00–%02d:00", peakHour, end)}"
        } else {
            stringResource(id = R.string.stats_phase_two_no_peak)
        }
        Text(
            text = peakSummary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Hour axis labels (0, 6, 12, 18, 23)
        Row(modifier = Modifier.fillMaxWidth().padding(start = 36.dp)) {
            val labelHours = listOf(0, 6, 12, 18)
            labelHours.forEachIndexed { idx, h ->
                Text(
                    text = if (h == 0) "12am" else if (h == 12) "12pm" else "${h}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))

        // Heatmap grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (weekday in 1..7) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = DayOfWeek.of(weekday).getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(32.dp)
                    )
                    val rowCells = grouped[weekday].orEmpty().sortedBy { it.hour }
                    rowCells.forEach { cell ->
                        HeatmapCellBox(
                            cell = cell,
                            isPeak = cell.weekday == peakWeekday && cell.hour == peakHour
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.stats_phase_two_less),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            for (step in 0..4) {
                val intensity = step / 4f
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(heatmapColor(intensity))
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(3.dp)
                        )
                )
            }
            Text(
                text = stringResource(id = R.string.stats_phase_two_more),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Last $windowDays days",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun HeatmapCellBox(cell: HeatmapCell, isPeak: Boolean) {
    val alpha = cell.intensity.coerceIn(0f, 1f)
    val borderColor = if (isPeak) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    }
    val borderWidth = if (isPeak) 1.5.dp else 0.5.dp
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(heatmapColor(alpha))
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(3.dp)
            )
    )
}

@Composable
private fun heatmapColor(intensity: Float): Color {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    return lerp(base, accent, intensity.coerceIn(0f, 1f))
}

/** Streaks insight: streak count + consistency score. */
@Composable
private fun StreaksInsightCard(insight: StreaksConsistencyInsight) {
    InsightCard {
        Text(
            text = stringResource(id = R.string.stats_phase_two_streaks_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Streak count
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "🔥", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = insight.controlledDaysStreak.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "day streak",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(48.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            )

            // Consistency score
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${insight.consistencyScore}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "consistency",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(48.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            )

            // Volatility
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "±${insight.volatilityMinutes}m",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = "from ${formatDuration(insight.baselineMinutes * 60_000L)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** App sprawl insight with delta badge. */
@Composable
private fun AppSprawlInsightCard(insight: AppSprawlInsight) {
    val deltaText = if (insight.deltaPercent == null) {
        null
    } else {
        val prefix = if (insight.deltaPercent >= 0) "+" else "−"
        "$prefix${insight.deltaPercent.absoluteValue}%"
    }
    val improved = insight.deltaPercent != null && insight.deltaPercent < 0
    val deltaChipColor = when {
        insight.deltaPercent == null -> MaterialTheme.colorScheme.surfaceVariant
        improved -> Color(0xFF1E6B3A)
        else -> Color(0xFF8B1A1A)
    }

    InsightCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(id = R.string.stats_phase_two_sprawl_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = String.format("%.1f", insight.avgDistinctAppsPerDay),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "apps/day",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
            if (deltaText != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(deltaChipColor)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = deltaText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top Apps
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppUsageListItem(
    appUsage: AppUsageData,
    totalUsageMillis: Long = 0L,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var appIcon by remember { mutableStateOf<Drawable?>(null) }

    LaunchedEffect(appUsage.packageName) {
        appIcon = loadAppIcon(context, appUsage.packageName)
    }

    val usagePercentage = if (totalUsageMillis > 0L) {
        (appUsage.usageDurationMillis.toFloat() / totalUsageMillis.toFloat())
    } else {
        0f
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconImage(
                drawable = appIcon,
                contentDescription = appUsage.appName,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = appUsage.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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

                val animatedFraction by animateFloatAsState(
                    targetValue = usagePercentage,
                    animationSpec = tween(500),
                    label = "appUsageBar_${appUsage.packageName}"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { animatedFraction },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        strokeCap = StrokeCap.Round
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
        val bitmap = remember(drawable) { drawable.toBitmap() }
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier
        )
    } else {
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

// ─────────────────────────────────────────────────────────────────────────────
// Phase 3 Insights
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PhaseThreeInsightsSection(insights: PhaseThreeInsights, range: StatsRange) {
    // Only show if at least one sub-insight is present
    if (insights.forecast == null && insights.correlation == null) return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.stats_phase_three_header),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        insights.forecast?.let { ForecastInsightCard(insight = it) }
        insights.correlation?.let { CorrelationInsightCard(insight = it) }
    }
}

@Composable
private fun ForecastInsightCard(insight: EndOfDayForecastInsight) {
    InsightCard {
        // ---- header row: title + confidence chip ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.stats_forecast_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ForecastConfidenceChip(confidence = insight.confidence)
        }

        Spacer(Modifier.height(12.dp))

        TodayForecastContent(insight = insight)
    }
}

@Composable
private fun TodayForecastContent(insight: EndOfDayForecastInsight) {
    val hasHistory = insight.weeksUsed > 0

    if (!hasHistory) {
        // Not enough history — show actual usage only with a note
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = formatDuration(insight.actualUsageMillis),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "used today",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.stats_forecast_no_history),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    // Hero: projected total
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = formatDuration(insight.forecastedTotalMillis),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.stats_forecast_projected).lowercase(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
    }

    Spacer(Modifier.height(10.dp))

    // Progress bar: used / forecasted
    val forecastFraction = if (insight.forecastedTotalMillis > 0L) {
        (insight.actualUsageMillis.toFloat() / insight.forecastedTotalMillis.toFloat())
            .coerceIn(0f, 1f)
    } else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = forecastFraction,
        animationSpec = tween(700),
        label = "forecastBar"
    )
    LinearProgressIndicator(
        progress = { animatedFraction },
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.primaryContainer,
        strokeCap = StrokeCap.Round
    )

    Spacer(Modifier.height(8.dp))

    // Used / Remaining sub-stats
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.stats_forecast_used_so_far, formatDuration(insight.actualUsageMillis)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (insight.remainingForecastMillis > 0L) {
            Text(
                text = stringResource(R.string.stats_forecast_remaining, formatDuration(insight.remainingForecastMillis)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Typical comparison
    insight.typicalSameWeekdayMillis?.let { typical ->
        Spacer(Modifier.height(10.dp))
        ForecastTypicalComparison(
            insight = insight,
            typical = typical,
            referenceMillis = insight.forecastedTotalMillis
        )
    }

    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(
            R.string.stats_forecast_based_on,
            insight.weeksUsed,
            insight.weekdayName + "s"
        ),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
    )
}

@Composable
private fun ForecastTypicalComparison(
    insight: EndOfDayForecastInsight,
    typical: Long,
    referenceMillis: Long
) {
    val delta = insight.typicalDeltaPercent
    val typicalText = stringResource(
        R.string.stats_forecast_typical_day,
        insight.weekdayName,
        formatDuration(typical)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = typicalText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (delta != null && abs(delta) >= 5) {
            val (chipBg, chipText, label) = when {
                delta > 0 -> Triple(
                    Color(0xFF8B1A1A),
                    Color.White,
                    "+${delta}%"
                )
                else -> Triple(
                    Color(0xFF1E6B3A),
                    Color.White,
                    "${delta}%"
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(chipBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = chipText
                )
            }
        } else if (delta != null) {
            Text(
                text = stringResource(R.string.stats_forecast_vs_typical_same, insight.weekdayName),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF1E6B3A)
            )
        }
    }
}

@Composable
private fun ForecastConfidenceChip(confidence: ForecastConfidence) {
    val (bg, fg, label) = when (confidence) {
        ForecastConfidence.HIGH -> Triple(
            Color(0xFF1E6B3A).copy(alpha = 0.12f),
            Color(0xFF1E6B3A),
            stringResource(R.string.stats_forecast_confidence_high)
        )
        ForecastConfidence.MEDIUM -> Triple(
            Color(0xFFC47A00).copy(alpha = 0.12f),
            Color(0xFFC47A00),
            stringResource(R.string.stats_forecast_confidence_medium)
        )
        ForecastConfidence.LOW -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            stringResource(R.string.stats_forecast_confidence_low)
        )
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = fg
        )
    }
}

@Composable
private fun CorrelationInsightCard(insight: CorrelationInsight) {
    InsightCard {
        // ---- header row: title + strength chip ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.stats_correlation_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            CorrelationStrengthChip(strength = insight.strength)
        }

        Spacer(Modifier.height(12.dp))

        // ---- R value + visual bar ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%.2f", insight.pearsonR),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = correlationColor(insight.pearsonR)
                )
                Text(
                    text = "r",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Centred correlation bar: negative left, positive right
            Column(modifier = Modifier.weight(1f)) {
                val barFraction = abs(insight.pearsonR).toFloat().coerceIn(0f, 1f)
                val animatedFraction by animateFloatAsState(
                    targetValue = barFraction,
                    animationSpec = tween(700),
                    label = "corrBar"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    // Filled portion from centre toward the appropriate side
                    Box(
                        modifier = Modifier
                            .align(
                                if (insight.pearsonR >= 0) Alignment.CenterStart
                                else Alignment.CenterEnd
                            )
                            .fillMaxWidth(animatedFraction / 2f)
                            .fillMaxSize()
                            .background(correlationColor(insight.pearsonR))
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "−1",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    )
                    Text(
                        text = "0",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    )
                    Text(
                        text = "+1",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ---- insight message ----
        val messageText = stringResource(
            id = when (insight.message) {
                CorrelationMessage.MORE_UNLOCKS_MORE_TIME -> R.string.stats_correlation_more_unlocks_more_time
                CorrelationMessage.UNLOCKS_NOT_PREDICTIVE -> R.string.stats_correlation_not_predictive
                CorrelationMessage.MORE_UNLOCKS_SHORTER_SESSIONS -> R.string.stats_correlation_shorter_sessions
            }
        )
        Text(
            text = messageText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = MaterialTheme.typography.bodySmall.fontSize * 1.5
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.stats_correlation_days_analyzed, insight.dayCount),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
        )
    }
}

@Composable
private fun CorrelationStrengthChip(strength: CorrelationStrength) {
    val (labelRes, bg, fg) = when (strength) {
        CorrelationStrength.STRONG -> Triple(
            R.string.stats_correlation_strength_strong,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        CorrelationStrength.MODERATE -> Triple(
            R.string.stats_correlation_strength_moderate,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        CorrelationStrength.WEAK -> Triple(
            R.string.stats_correlation_strength_weak,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        CorrelationStrength.NONE -> Triple(
            R.string.stats_correlation_strength_none,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = fg
        )
    }
}

@Composable
private fun correlationColor(r: Double): Color {
    return when {
        r > 0.35  -> MaterialTheme.colorScheme.error
        r < -0.25 -> MaterialTheme.colorScheme.tertiary
        else      -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Utility
// ─────────────────────────────────────────────────────────────────────────────

/** Simple 4-value tuple to avoid destructuring workarounds. */
private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
