package com.librefocus.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.librefocus.ui.home.AppUsageTrendData
import com.librefocus.ui.home.DailyTrendData
import com.librefocus.ui.home.TrendComparison
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberEnd
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@Composable
fun TrendsSection(
    dailyTrends: List<DailyTrendData>,
    topAppsUsage: List<AppUsageTrendData>,
    screenTimeComparison: TrendComparison?,
    unlocksComparison: TrendComparison?,
    modifier: Modifier = Modifier
) {
    if (dailyTrends.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        screenTimeComparison?.let {
            if (it.actionableAdvice.isNotEmpty()) {
                TrendInsightCard(
                    title = "Screen Time Insight",
                    comparison = it
                )
            }
        }

        //ScreenTimeTrendChart(dailyTrends)

        unlocksComparison?.let {
            if (it.actionableAdvice.isNotEmpty()) {
                TrendInsightCard(
                    title = "Unlocks Insight",
                    comparison = it
                )
            }
        }

        //UnlocksTrendChart(dailyTrends)

        if (topAppsUsage.isNotEmpty()) {
            AppUsageDistributionCard(topAppsUsage)
        }
    }
}

@Composable
fun TrendInsightCard(title: String, comparison: TrendComparison) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = if (comparison.percentageChange == 0) Icons.Default.Info
            else if (comparison.isIncrease) Icons.Default.TrendingUp
            else Icons.Default.TrendingDown

            val color = if (comparison.percentageChange == 0) MaterialTheme.colorScheme.onTertiaryContainer
            else if (comparison.isIncrease) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = comparison.actionableAdvice,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun ScreenTimeTrendChart(dailyTrends: List<DailyTrendData>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Screen Time (Week)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val chartModelProducer = remember { CartesianChartModelProducer() }
            val yValues = remember(dailyTrends) {
                dailyTrends.map { it.screenTimeMillis.toDouble() / TimeUnit.HOURS.toMillis(1).toDouble() }
            }
            val bottomLabels = remember(dailyTrends) {
                dailyTrends.map { it.dayOfWeek }
            }

            LaunchedEffect(yValues) {
                chartModelProducer.runTransaction {
                    columnSeries {
                        val values = if (yValues.isEmpty()) listOf(0.0) else yValues
                        series(values)
                    }
                }
            }

            val columnLayer = rememberColumnCartesianLayer(
                columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                    listOf(
                        rememberLineComponent(
                            fill = fill(MaterialTheme.colorScheme.primary),
                            thickness = 16.dp,
                            shape = CorneredShape(
                                topLeft = CorneredShape.Corner.Relative(50, CorneredShape.CornerTreatment.Rounded),
                                topRight = CorneredShape.Corner.Relative(50, CorneredShape.CornerTreatment.Rounded)
                            )
                        )
                    )
                ),
                columnCollectionSpacing = 6.dp
            )

            val bottomAxisFormatter = remember(bottomLabels) {
                CartesianValueFormatter { _, value, _ ->
                    bottomLabels.getOrNull(value.roundToInt()) ?: ""
                }
            }

            val chart = rememberCartesianChart(
                columnLayer,
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = bottomAxisFormatter,
                    guideline = null
                ),
                endAxis = VerticalAxis.rememberEnd()
            )

            CartesianChartHost(
                chart = chart,
                modelProducer = chartModelProducer,
                modifier = Modifier.fillMaxWidth().height(200.dp),
                scrollState = rememberVicoScrollState(scrollEnabled = false)
            )
        }
    }
}

@Composable
fun UnlocksTrendChart(dailyTrends: List<DailyTrendData>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Device Unlocks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val chartModelProducer = remember { CartesianChartModelProducer() }
            val yValues = remember(dailyTrends) {
                dailyTrends.map { it.unlocks.toDouble() }
            }
            val bottomLabels = remember(dailyTrends) {
                dailyTrends.map { it.dayOfWeek }
            }

            LaunchedEffect(yValues) {
                chartModelProducer.runTransaction {
                    lineSeries {
                        val values = if (yValues.isEmpty()) listOf(0.0) else yValues
                        series(values)
                    }
                }
            }

            val lineLayer = rememberLineCartesianLayer()

            val bottomAxisFormatter = remember(bottomLabels) {
                CartesianValueFormatter { _, value, _ ->
                    bottomLabels.getOrNull(value.roundToInt()) ?: ""
                }
            }

            val chart = rememberCartesianChart(
                lineLayer,
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = bottomAxisFormatter,
                    guideline = null
                ),
                endAxis = VerticalAxis.rememberEnd()
            )

            CartesianChartHost(
                chart = chart,
                modelProducer = chartModelProducer,
                modifier = Modifier.fillMaxWidth().height(200.dp),
                scrollState = rememberVicoScrollState(scrollEnabled = false)
            )
        }
    }
}

@Composable
fun AppUsageDistributionCard(topAppsUsage: List<AppUsageTrendData>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Top App Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val maxUsage = topAppsUsage.maxOfOrNull { it.usageMillis } ?: 1L

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                topAppsUsage.forEach { app ->
                    val fraction = if (maxUsage > 0) app.usageMillis.toFloat() / maxUsage.toFloat() else 0f
                    val durationStr = formatDuration(app.usageMillis)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = app.appName,
                            modifier = Modifier.width(80.dp),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            )
                        }
                        Text(
                            text = durationStr,
                            modifier = Modifier.width(60.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalMinutes = millis / (1000 * 60)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m"
    }
}
