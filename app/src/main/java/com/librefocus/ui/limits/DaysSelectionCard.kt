package com.librefocus.ui.limits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.librefocus.models.DayOfWeek

@Composable
fun DaysSelectionCard(
    isAllWeek: Boolean,
    selectedDays: Set<DayOfWeek>,
    onAllWeekChange: (Boolean) -> Unit,
    onDayToggle: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "All Week",
                    style = MaterialTheme.typography.titleSmall
                )
                Switch(
                    checked = isAllWeek,
                    onCheckedChange = onAllWeekChange
                )
            }

            if (!isAllWeek) {
                HorizontalDivider()
                
                Text(
                    text = "Select Days",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                DayChipsRow(
                    selectedDays = selectedDays,
                    onDayToggle = onDayToggle
                )
            }
        }
    }
}

@Composable
private fun DayChipsRow(
    selectedDays: Set<DayOfWeek>,
    onDayToggle: (DayOfWeek) -> Unit
) {
    val days = listOf(
        DayOfWeek.MON to "Mon",
        DayOfWeek.TUE to "Tue",
        DayOfWeek.WED to "Wed",
        DayOfWeek.THU to "Thu",
        DayOfWeek.FRI to "Fri",
        DayOfWeek.SAT to "Sat",
        DayOfWeek.SUN to "Sun"
    )

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        days.forEach { (day, label) ->
            FilterChip(
                selected = day in selectedDays,
                onClick = { onDayToggle(day) },
                label = { Text(label) }
            )
        }
    }
}
