package com.librefocus.ui.stats

import android.text.Layout
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
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
import com.librefocus.models.UsageValuePoint
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberEnd
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.insets
import com.patrykandpatrick.vico.compose.common.shape.markerCorneredShape
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
import com.patrykandpatrick.vico.core.common.LayeredComponent
import com.patrykandpatrick.vico.core.common.component.ShapeComponent
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import com.patrykandpatrick.vico.core.common.shape.CorneredShape.Corner
import com.patrykandpatrick.vico.core.common.shape.CorneredShape.CornerTreatment
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@Composable
internal fun rememberMarker(
    valueFormatter: DefaultCartesianMarker.ValueFormatter = DefaultCartesianMarker.ValueFormatter.default(),
    showIndicator: Boolean = true,
): CartesianMarker {
    val labelBackgroundShape = markerCorneredShape(Corner.Rounded)
    val labelBackground =
        rememberShapeComponent(
            fill = fill(MaterialTheme.colorScheme.background),
            shape = labelBackgroundShape,
            strokeThickness = 1.dp,
            strokeFill = fill(MaterialTheme.colorScheme.outline),
        )
    val label =
        rememberTextComponent(
            color = MaterialTheme.colorScheme.onSurface,
            textAlignment = Layout.Alignment.ALIGN_CENTER,
            padding = insets(8.dp, 4.dp),
            background = labelBackground,
        )
    val indicatorFrontComponent =
        rememberShapeComponent(fill(MaterialTheme.colorScheme.surface), CorneredShape.Pill)
    val guideline = rememberAxisGuidelineComponent()
    return rememberDefaultCartesianMarker(
        label = label,
        valueFormatter = valueFormatter,
        indicator =
            if (showIndicator) {
                { color ->
                    LayeredComponent(
                        back = ShapeComponent(fill(color.copy(alpha = 0.15f)), CorneredShape.Pill),
                        front =
                            LayeredComponent(
                                back = ShapeComponent(
                                    fill = fill(color),
                                    shape = CorneredShape.Pill
                                ),
                                front = indicatorFrontComponent,
                                padding = insets(5.dp),
                            ),
                        padding = insets(10.dp),
                    )
                }
            } else {
                null
            },
        indicatorSize = 36.dp,
        guideline = guideline,
    )
}

@Composable
fun UsageChartCard(
    usagePoints: List<UsageValuePoint>,
    metric: StatsMetric,
    range: StatsRange,
    formatted: com.librefocus.utils.FormattedDateTimePreferences? = null
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
    val bottomLabels = remember(usagePoints, range, formatted) {
        usagePoints.map { point ->
            formatBottomLabel(point, range, formatted)
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

    val markerValueFormatter = remember(usagePoints, metric, range, formatted) {
        DefaultCartesianMarker.ValueFormatter { _, targets ->
            val index = targets.firstOrNull()?.x?.roundToInt() ?: return@ValueFormatter ""
            val point = usagePoints.getOrNull(index) ?: return@ValueFormatter ""
            val valueText = when (metric) {
                StatsMetric.ScreenTime -> formatDuration(point.totalUsageMillis)
                StatsMetric.Opens -> "${point.totalLaunchCount} opens"
            }
            val timeLabel = point.markerTimeLabel(range, formatted)
            "$valueText – $timeLabel"
        }
    }
    val marker = rememberMarker(valueFormatter = markerValueFormatter)


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
        markerController = CartesianMarkerController.ShowOnPress
    )
    val vicoTheme = rememberM3VicoTheme()

    // Chart only - no UI decorations
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

private fun UsageValuePoint.markerTimeLabel(
    range: StatsRange,
    formatted: com.librefocus.utils.FormattedDateTimePreferences?
): String {
    if (formatted == null) {
        // Fallback to system default
        val zone = java.time.ZoneId.systemDefault()
        val start = java.time.Instant.ofEpochMilli(bucketStartUtc).atZone(zone)
        return when (range) {
            StatsRange.Day -> markerHourFormatterFallback.format(start)
            StatsRange.Week, StatsRange.Month, StatsRange.Custom -> markerDayFormatterFallback.format(start)
        }
    }
    
    return when (range) {
        StatsRange.Day -> formatted.formatTime(bucketStartUtc)
        StatsRange.Week, StatsRange.Month, StatsRange.Custom -> formatted.formatDate(bucketStartUtc)
    }
}

private fun UsageValuePoint.markerRangeLabel(
    range: StatsRange,
    formatted: com.librefocus.utils.FormattedDateTimePreferences?
): String {
    if (formatted == null) {
        // Fallback to system default
        val zone = java.time.ZoneId.systemDefault()
        val start = java.time.Instant.ofEpochMilli(bucketStartUtc).atZone(zone)
        val end = start.plusHours(range.markerBucketDurationHours())
        return when (range) {
            StatsRange.Day -> "${markerHourFormatterFallback.format(start)} - ${markerHourFormatterFallback.format(end)}"
            StatsRange.Week, StatsRange.Month, StatsRange.Custom -> "${markerDayFormatterFallback.format(start)} - ${markerDayFormatterFallback.format(end)}"
        }
    }
    
    val endMillis = bucketStartUtc + java.util.concurrent.TimeUnit.HOURS.toMillis(range.markerBucketDurationHours())
    return when (range) {
        StatsRange.Day -> formatted.formatHourRange(bucketStartUtc, endMillis)
        StatsRange.Week, StatsRange.Month, StatsRange.Custom -> {
            "${formatted.formatShortDate(bucketStartUtc)} - ${formatted.formatShortDate(endMillis)}"
        }
    }
}

private fun StatsRange.markerBucketDurationHours(): Long {
    return when (this) {
        StatsRange.Day -> 1L
        StatsRange.Week, StatsRange.Month, StatsRange.Custom -> 24L
    }
}

// Fallback formatters for when preferences are not yet loaded
private val markerHourFormatterFallback: java.time.format.DateTimeFormatter =
    java.time.format.DateTimeFormatter.ofPattern("h:mm a")

private val markerDayFormatterFallback: java.time.format.DateTimeFormatter =
    java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy")
