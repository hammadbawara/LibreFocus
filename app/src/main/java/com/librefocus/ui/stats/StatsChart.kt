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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.librefocus.models.UsageValuePoint
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberEnd
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarkerController
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarkerVisibilityListener
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import com.patrykandpatrick.vico.core.common.shape.CorneredShape.Corner
import com.patrykandpatrick.vico.core.common.shape.CorneredShape.CornerTreatment
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@Composable
fun UsageChartCard(
    usagePoints: List<UsageValuePoint>,
    metric: StatsMetric,
    range: StatsRange
) {
    val chartModelProducer = remember { CartesianChartModelProducer() }
    val haptics = LocalHapticFeedback.current
    var lastHighlightedIndex by remember { mutableStateOf<Int?>(null) }
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
    val columnThickness = remember(usagePoints.size) {
        when {
            usagePoints.size <= 6 -> 20.dp
            usagePoints.size <= 12 -> 14.dp
            usagePoints.size <= 24 -> 10.dp
            else -> 8.dp
        }
    }
    val columnShape = remember {
        CorneredShape(
            topLeft = Corner.Relative(50, CornerTreatment.Rounded),
            topRight = Corner.Relative(50, CornerTreatment.Rounded)
        )
    }
    val columnColor = MaterialTheme.colorScheme.primary
    val columnLayer = rememberColumnCartesianLayer(
        columnProvider = ColumnCartesianLayer.ColumnProvider.series(
            listOf(
                rememberLineComponent(
                    fill = fill(columnColor),
                    thickness = columnThickness,
                    shape = columnShape
                )
            )
        ),
        columnCollectionSpacing = 6.dp
    )
    val scrollState = rememberVicoScrollState(scrollEnabled = false)

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

    val markerValueFormatter = remember(usagePoints, metric, range) {
        DefaultCartesianMarker.ValueFormatter { _, targets ->
            val index = targets.firstOrNull()?.x?.roundToInt() ?: return@ValueFormatter ""
            val point = usagePoints.getOrNull(index) ?: return@ValueFormatter ""
            val valueText = when (metric) {
                StatsMetric.ScreenTime -> formatDuration(point.totalUsageMillis)
                StatsMetric.Opens -> "${point.totalLaunchCount} opens"
            }
            val rangeText = point.markerRangeLabel(range)
            "$valueText\n$rangeText"
        }
    }
    val markerLabel = rememberTextComponent(
        color = MaterialTheme.colorScheme.onSurface,
        textSize = 12.sp,
        background = null
    )
    val marker = rememberDefaultCartesianMarker(
        label = markerLabel,
        valueFormatter = markerValueFormatter
    )
    val markerVisibilityListener = remember(haptics) {
        object : CartesianMarkerVisibilityListener {
            override fun onShown(marker: CartesianMarker, targets: List<CartesianMarker.Target>) {
                maybeTriggerHaptics(targets)
            }

            override fun onUpdated(marker: CartesianMarker, targets: List<CartesianMarker.Target>) {
                maybeTriggerHaptics(targets)
            }

            override fun onHidden(marker: CartesianMarker) {
                lastHighlightedIndex = null
            }

            private fun maybeTriggerHaptics(targets: List<CartesianMarker.Target>) {
                val index = targets.firstOrNull()?.x?.roundToInt()
                if (index != null && index != lastHighlightedIndex) {
                    lastHighlightedIndex = index
                    //haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
        }
    }
    val chart = rememberCartesianChart(
        columnLayer,
        endAxis = VerticalAxis.rememberEnd(
            valueFormatter = startAxisFormatter,
        ),
        bottomAxis = HorizontalAxis.rememberBottom(
            valueFormatter = bottomAxisFormatter,
            guideline = null
        ),
        marker = marker,
        markerVisibilityListener = markerVisibilityListener,
        markerController = CartesianMarkerController.ShowOnPress
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
                    scrollState = scrollState,
                    consumeMoveEvents = true
                )
            }
        }
    }
}

private fun UsageValuePoint.markerRangeLabel(range: StatsRange): String {
    val zone = java.time.ZoneId.systemDefault()
    val start = java.time.Instant.ofEpochMilli(bucketStartUtc).atZone(zone)
    val end = start.plusHours(range.markerBucketDurationHours())
    return when (range) {
        StatsRange.Day -> "${markerHourFormatter.format(start)} - ${markerHourFormatter.format(end)}"
        StatsRange.Week, StatsRange.Month, StatsRange.Custom -> "${markerDayFormatter.format(start)} - ${markerDayFormatter.format(end)}"
    }
}

private fun StatsRange.markerBucketDurationHours(): Long {
    return when (this) {
        StatsRange.Day -> 1L
        StatsRange.Week, StatsRange.Month, StatsRange.Custom -> 24L
    }
}

private val markerHourFormatter: java.time.format.DateTimeFormatter =
    java.time.format.DateTimeFormatter.ofPattern("HH:mm")

private val markerDayFormatter: java.time.format.DateTimeFormatter =
    java.time.format.DateTimeFormatter.ofPattern("dd MMM")
