package com.librefocus.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.librefocus.ui.home.AnalyticsInsights
import com.librefocus.ui.home.HeatmapData

@Composable
fun AnalyticsSection(
    heatmapData: HeatmapData,
    insights: AnalyticsInsights,
    modifier: Modifier = Modifier
) {
    if (heatmapData.dailyHourlyUsage.isEmpty() && insights.totalUsageMillis == 0L) {
        return // Do not show analytics if no data exists
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HighlightsCards(insights = insights)
        HeatmapCard(heatmapData = heatmapData)
    }
}

@Composable
fun HighlightsCards(insights: AnalyticsInsights) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Total usage card
        HighlightCard(
            modifier = Modifier.weight(1f),
            title = "Week Total",
            value = formatDuration(insights.totalUsageMillis)
        )
        // Peak Hour
        val peakHourStr = insights.peakHour?.let { 
            "${it.toString().padStart(2, '0')}:00" 
        } ?: "--:--"
        HighlightCard(
            modifier = Modifier.weight(1f),
            title = "Peak Hour",
            value = peakHourStr
        )
        // Top App
        HighlightCard(
            modifier = Modifier.weight(1f),
            title = "Top App",
            value = insights.topAppName ?: "None",
            isEllipsis = true
        )
    }
}

@Composable
fun HeatmapCard(heatmapData: HeatmapData) {
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
                text = "Screen Time Heatmap",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            HeatmapChart(data = heatmapData)
        }
    }
}

@Composable
fun HeatmapChart(data: HeatmapData) {
    // 7 days (rows M-S) x 24 hours (columns 0-23)
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    
    // Background color of inactive cells and axis text
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Option to display hours at top
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("00", "06", "12", "18", "23").forEach {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        days.forEachIndexed { index, dayLabel ->
            val dayOfWeek = index + 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Y-axis label
                Text(
                    text = dayLabel,
                    modifier = Modifier.width(24.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                // 24 cells for each hour
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (hour in 0..23) {
                        val usageMap = data.dailyHourlyUsage[dayOfWeek]
                        val usage = usageMap?.get(hour) ?: 0L
                        val intensity = if (data.maxUsagePerHour > 0) {
                            usage.toFloat() / data.maxUsagePerHour.toFloat()
                        } else {
                            0f
                        }
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(1.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(getHeatmapColor(intensity))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun getHeatmapColor(intensity: Float): Color {
    val baseColor = MaterialTheme.colorScheme.primary
    val emptyAlpha = 0.08f // Soft background for empty slots
    
    // Non-linear scaling to make small usages visible without losing distinction of high usages
    val adjustedIntensity = if (intensity > 0) {
        0.3f + (intensity * 0.7f) // Minimum 0.3 alpha if there's any usage
    } else {
        emptyAlpha
    }
    
    return baseColor.copy(alpha = minOf(1f, adjustedIntensity))
}

@Composable
fun HighlightCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    isEllipsis: Boolean = false
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = if (isEllipsis) TextOverflow.Ellipsis else TextOverflow.Visible
            )
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
