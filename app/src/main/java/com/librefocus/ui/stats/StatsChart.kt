package com.librefocus.ui.stats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.librefocus.models.UsageValuePoint
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@Composable
fun UsageChartCard(
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
            columnSeries {
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
            if (bottomLabels.isEmpty()) "No data" else bottomLabels.getOrNull(value.roundToInt()) ?: "N/A"
        }
    }

    val lineLayer = rememberColumnCartesianLayer()
    val chart = rememberCartesianChart(
        lineLayer,
        startAxis = VerticalAxis.rememberStart(
            valueFormatter = startAxisFormatter,
            guideline = null
        ),
        bottomAxis = HorizontalAxis.rememberBottom(
            valueFormatter = bottomAxisFormatter,
            guideline = null
        )
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
                        .height(200.dp),
                )
            }
        }
    }
}
