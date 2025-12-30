package com.librefocus.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.librefocus.data.repository.UsageTrackingRepository
import com.librefocus.models.UsageValuePoint
import com.librefocus.utils.DateTimeFormatterManager
import com.librefocus.utils.FormattedDateTimePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

enum class StatsMetric {
    ScreenTime,
    Opens
}

enum class StatsRange {
    Day,
    Week,
    Month,
    Custom
}

data class StatsContentUiState(
    val selectedRangeLabel: String = "Today",
    val totalUsageMillis: Long = 0L,
    val averageSessionMillis: Long = 0L,
    val usagePoints: List<UsageValuePoint> = emptyList(),
    val isLoading: Boolean = false,
    val totalDisplayValue: String = "0m",
    val totalDisplayLabel: String = "Total",
    val averageDisplayValue: String = "0m",
    val averageDisplayLabel: String = "Avg per hour",
    val errorMessage: String? = null,
)

open class StatsContentViewModel(
    private val usageRepository: UsageTrackingRepository,
    private val dateTimeFormatterManager: DateTimeFormatterManager,
    private val packageName: String? = null // null = all apps, non-null = specific app
): ViewModel() {

    private val _metric = MutableStateFlow(StatsMetric.ScreenTime)
    val metric: StateFlow<StatsMetric> = _metric

    private val _range = MutableStateFlow(StatsRange.Day)
    val range: StateFlow<StatsRange> = _range

    private val _uiState = MutableStateFlow(StatsContentUiState(isLoading = true))
    val uiState: StateFlow<StatsContentUiState> = _uiState

    private var customRange: StatsPeriodState? = null

    /**
     * Flow of formatted date/time preferences.
     * UI should observe this to react to preference changes.
     */
    val formattedPreferences: StateFlow<FormattedDateTimePreferences?> =
        dateTimeFormatterManager.formattedPreferences
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

    private val _periodState = MutableStateFlow(initialPeriodStateDefault())
    val periodState: StateFlow<StatsPeriodState> = _periodState

    init {
        // Update period labels when formatted preferences change
        viewModelScope.launch {
            formattedPreferences.collect { formatted ->
                if (formatted != null) {
                    val current = _periodState.value
                    // Update labels with new formatting preferences
                    _periodState.value = when (_range.value) {
                        StatsRange.Day -> periodForDay(formatted.toZonedDateTime(current.startUtc), formatted)
                        StatsRange.Week -> periodForWeek(formatted.toZonedDateTime(current.startUtc), formatted)
                        StatsRange.Month -> periodForMonth(formatted.toZonedDateTime(current.startUtc), formatted)
                        StatsRange.Custom -> current.copy(
                            label = formatRangeLabel(current.startUtc, current.endUtc - current.startUtc, formatted)
                        )
                    }
                    refreshData()
                }
            }
        }
    }

    fun onMetricSelected(newMetric: StatsMetric) {
        if (newMetric != _metric.value) {
            _metric.value = newMetric
            refreshData()
        }
    }

    fun onRangeSelected(range: StatsRange) {
        val formatted = formattedPreferences.value ?: return
        if (range == _range.value && range != StatsRange.Custom) return
        _range.value = range
        val nowLocal = ZonedDateTime.now(formatted.zoneId)
        val newPeriod = when (range) {
            StatsRange.Day -> periodForDay(nowLocal, formatted)
            StatsRange.Week -> periodForWeek(nowLocal, formatted)
            StatsRange.Month -> periodForMonth(nowLocal, formatted)
            StatsRange.Custom -> customRange ?: periodForWeek(nowLocal, formatted)
        }
        _periodState.value = newPeriod
        if (range == StatsRange.Custom) {
            customRange = newPeriod
        }
        refreshData()
    }

    fun onNavigatePrevious() {
        val formatted = formattedPreferences.value ?: return
        val current = _periodState.value
        val durationMillis = current.endUtc - current.startUtc
        val newPeriod = when (_range.value) {
            StatsRange.Day -> {
                val previousDay = formatted.toLocalDate(current.startUtc).minusDays(1)
                periodForDate(previousDay, formatted)
            }
            StatsRange.Week -> {
                val newStartUtc = current.startUtc - TimeUnit.DAYS.toMillis(7)
                val newEndUtc = current.endUtc - TimeUnit.DAYS.toMillis(7)
                StatsPeriodState(
                    startUtc = newStartUtc,
                    endUtc = newEndUtc,
                    label = formatRangeLabel(newStartUtc, durationMillis, formatted)
                )
            }
            StatsRange.Month -> {
                val previousMonth = formatted.toLocalDate(current.startUtc).minusMonths(1)
                periodForMonth(previousMonth.atStartOfDay(formatted.zoneId), formatted)
            }
            StatsRange.Custom -> customRange?.let {
                val length = it.endUtc - it.startUtc
                val newStart = current.startUtc - length
                StatsPeriodState(
                    startUtc = newStart,
                    endUtc = newStart + length,
                    label = formatRangeLabel(newStart, length, formatted)
                )
            } ?: current
        }
        _periodState.value = newPeriod
        if (_range.value == StatsRange.Custom) {
            customRange = newPeriod
        }
        refreshData()
    }

    fun onNavigateNext() {
        val formatted = formattedPreferences.value ?: return
        val current = _periodState.value
        val durationMillis = current.endUtc - current.startUtc
        val todayLocal = LocalDate.now(formatted.zoneId)
        val todayStartUtc = todayLocal.atStartOfDay(formatted.zoneId).toInstant().toEpochMilli()

        val newPeriod = when (_range.value) {
            StatsRange.Day -> {
                val nextDay = formatted.toLocalDate(current.startUtc).plusDays(1)
                val nextDayStartUtc = nextDay.atStartOfDay(formatted.zoneId).toInstant().toEpochMilli()
                if (nextDayStartUtc > todayStartUtc) return
                periodForDate(nextDay, formatted)
            }
            StatsRange.Week -> {
                val candidateStartUtc = current.startUtc + TimeUnit.DAYS.toMillis(7)
                if (candidateStartUtc >= todayStartUtc + TimeUnit.DAYS.toMillis(1)) return
                StatsPeriodState(
                    startUtc = candidateStartUtc,
                    endUtc = candidateStartUtc + durationMillis,
                    label = formatRangeLabel(candidateStartUtc, durationMillis, formatted)
                )
            }
            StatsRange.Month -> {
                val nextMonth = formatted.toLocalDate(current.startUtc).plusMonths(1)
                val nextMonthStartUtc = nextMonth.atStartOfDay(formatted.zoneId).toInstant().toEpochMilli()
                if (nextMonthStartUtc >= todayStartUtc + TimeUnit.DAYS.toMillis(1)) return
                periodForMonth(nextMonth.atStartOfDay(formatted.zoneId), formatted)
            }
            StatsRange.Custom -> {
                val length = current.endUtc - current.startUtc
                val candidateStart = current.startUtc + length
                if (candidateStart >= todayStartUtc + TimeUnit.DAYS.toMillis(1)) return
                val custom = StatsPeriodState(
                    startUtc = candidateStart,
                    endUtc = candidateStart + length,
                    label = formatRangeLabel(candidateStart, length, formatted)
                )
                customRange = custom
                custom
            }
        }
        _periodState.value = newPeriod
        refreshData()
    }

    fun onCustomRangeSelected(startLocalMillis: Long, endLocalMillis: Long) {
        val formatted = formattedPreferences.value ?: return
        // Convert local timestamps to UTC for storage and querying
        val startLocalDate = formatted.toLocalDate(startLocalMillis)
        val endLocalDate = formatted.toLocalDate(endLocalMillis)

        val startUtc = startLocalDate.atStartOfDay(formatted.zoneId).toInstant().toEpochMilli()
        val endUtc = endLocalDate.plusDays(1).atStartOfDay(formatted.zoneId).toInstant().toEpochMilli()

        if (endUtc <= startUtc) {
            _uiState.update { it.copy(errorMessage = "Invalid range selection.") }
            return
        }

        val label = formatCustomLabel(startUtc, endUtc, formatted)
        val period = StatsPeriodState(startUtc, endUtc, label)
        customRange = period
        _range.value = StatsRange.Custom
        _periodState.value = period
        refreshData()
    }

    private fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            //runCatching {
                val period = _periodState.value  // Now guaranteed non-null
                val metric = _metric.value

                // Query data from repository using UTC timestamps
                val rawUsagePoints = if (packageName != null) {
                    // Query for specific app
                    when (_range.value) {
                        StatsRange.Day -> usageRepository.getAppUsageTotalsGroupedByHour(
                            packageName,
                            period.startUtc,
                            period.endUtc
                        )
                        StatsRange.Week, StatsRange.Month, StatsRange.Custom -> usageRepository.getAppUsageTotalsGroupedByDay(
                            packageName,
                            period.startUtc,
                            period.endUtc
                        )
                    }
                } else {
                    // Query for all apps
                    when (_range.value) {
                        StatsRange.Day -> usageRepository.getUsageTotalsGroupedByHour(
                            period.startUtc,
                            period.endUtc
                        )
                        StatsRange.Week, StatsRange.Month, StatsRange.Custom -> usageRepository.getUsageTotalsGroupedByDay(
                            period.startUtc,
                            period.endUtc
                        )
                    }
                }

                // Keep data in UTC - no conversion needed
                // Timezone conversion happens only at UI layer for display
                val filledUsagePoints = fillMissingUsagePoints(rawUsagePoints, period, _range.value)



                val totalUsageMillis = filledUsagePoints.sumOf { it.totalUsageMillis }

                val activeUsageBuckets = filledUsagePoints.count { it.totalUsageMillis > 0 }
                val averageSessionMillis = if (activeUsageBuckets > 0) {
                    totalUsageMillis / activeUsageBuckets
                } else {
                    0L
                }

                val selectedLabel = when (_range.value) {
                    StatsRange.Custom -> customRange?.label ?: period.label
                    else -> period.label
                }

                // Calculate total and average using utility functions
                val totalValue = calculateTotal(filledUsagePoints, metric)
                val averageValue = calculateAverage(filledUsagePoints, metric, _range.value)

                val totalDisplayValue = when (metric) {
                    StatsMetric.ScreenTime -> formatDuration(totalValue)
                    StatsMetric.Opens -> totalValue.toString()
                }

                val averageDisplayValue = when (metric) {
                    StatsMetric.ScreenTime -> formatDuration(averageValue)
                    StatsMetric.Opens -> averageValue.toString()
                }

                val totalDisplayLabel = formatTotalLabel(metric)
                val averageDisplayLabel = formatAverageLabel(_range.value, metric)

                StatsContentUiState(
                    selectedRangeLabel = selectedLabel,
                    totalUsageMillis = totalUsageMillis,
                    averageSessionMillis = averageSessionMillis,
                    usagePoints = filledUsagePoints,
                    isLoading = false,
                    totalDisplayValue = totalDisplayValue,
                    totalDisplayLabel = totalDisplayLabel,
                    averageDisplayValue = averageDisplayValue,
                    averageDisplayLabel = averageDisplayLabel
                )
//            }.onSuccess { state ->
//                _uiState.value = state
//            }.onFailure { throwable ->
//                _uiState.update { it.copy(isLoading = false, errorMessage = throwable.message) }
//            }
        }
    }

    private fun initialPeriodStateDefault(): StatsPeriodState {
        val now = System.currentTimeMillis()
        val systemZone = java.time.ZoneId.systemDefault()
        val dayStart = java.time.Instant.ofEpochMilli(now)
            .atZone(systemZone)
            .toLocalDate()
            .atStartOfDay(systemZone)

        val dayStartUtc = dayStart.toInstant().toEpochMilli()
        val dayEndUtc = dayStart.plusDays(1).toInstant().toEpochMilli()

        return StatsPeriodState(
            startUtc = dayStartUtc,
            endUtc = dayEndUtc,
            label = "Today"
        )
    }

    private fun periodForDay(localDateTime: ZonedDateTime, formatted: FormattedDateTimePreferences): StatsPeriodState {
        val dayStartLocal = localDateTime.toLocalDate().atStartOfDay(formatted.zoneId)
        val dayStartUtc = dayStartLocal.toInstant().toEpochMilli()
        val dayEndUtc = dayStartLocal.plusDays(1).toInstant().toEpochMilli()
        val label = formatted.formatDayLabel(dayStartUtc)
        return StatsPeriodState(
            startUtc = dayStartUtc,
            endUtc = dayEndUtc,
            label = label
        )
    }

    private fun periodForDate(localDate: LocalDate, formatted: FormattedDateTimePreferences): StatsPeriodState {
        val dayStartLocal = localDate.atStartOfDay(formatted.zoneId)
        val dayStartUtc = dayStartLocal.toInstant().toEpochMilli()
        val dayEndUtc = dayStartLocal.plusDays(1).toInstant().toEpochMilli()
        val label = formatted.formatDayLabel(dayStartUtc)
        return StatsPeriodState(
            startUtc = dayStartUtc,
            endUtc = dayEndUtc,
            label = label
        )
    }

    private fun periodForWeek(localDateTime: ZonedDateTime, formatted: FormattedDateTimePreferences): StatsPeriodState {
        val dayStartLocal = localDateTime.toLocalDate().atStartOfDay(formatted.zoneId)
        val weekStartLocal = dayStartLocal.minusDays(6)
        val weekStartUtc = weekStartLocal.toInstant().toEpochMilli()
        val weekEndUtc = weekStartLocal.plusDays(7).toInstant().toEpochMilli()
        val label = formatRangeLabel(weekStartUtc, TimeUnit.DAYS.toMillis(7), formatted)
        return StatsPeriodState(
            startUtc = weekStartUtc,
            endUtc = weekEndUtc,
            label = label
        )
    }

    private fun periodForMonth(localDateTime: ZonedDateTime, formatted: FormattedDateTimePreferences): StatsPeriodState {
        val monthStartLocal = localDateTime.withDayOfMonth(1).toLocalDate().atStartOfDay(formatted.zoneId)
        val nextMonthStartLocal = monthStartLocal.plusMonths(1)
        val monthStartUtc = monthStartLocal.toInstant().toEpochMilli()
        val monthEndUtc = nextMonthStartLocal.toInstant().toEpochMilli()
        val label = formatted.formatMonthLabel(monthStartUtc)
        return StatsPeriodState(
            startUtc = monthStartUtc,
            endUtc = monthEndUtc,
            label = label
        )
    }

    private fun formatRangeLabel(startUtc: Long, durationMillis: Long, formatted: FormattedDateTimePreferences): String {
        return formatted.formatDateRange(startUtc, startUtc + durationMillis)
    }

    private fun formatCustomLabel(startUtc: Long, endUtc: Long, formatted: FormattedDateTimePreferences): String {
        return formatted.formatDateRange(startUtc, endUtc)
    }

    private fun fillMissingUsagePoints(
        points: List<UsageValuePoint>,
        period: StatsPeriodState,
        range: StatsRange
    ): List<UsageValuePoint> {
        val bucketSizeMillis = when (range) {
            StatsRange.Day -> TimeUnit.HOURS.toMillis(1)
            StatsRange.Week, StatsRange.Month, StatsRange.Custom -> TimeUnit.DAYS.toMillis(1)
        }

        // Work with UTC timestamps throughout - no conversion
        val filteredPoints = points.filter { point ->
            point.bucketStartUtc >= period.startUtc && point.bucketStartUtc < period.endUtc
        }

        if (filteredPoints.isEmpty()) {
            return generateEmptySeries(period.startUtc, period.endUtc, bucketSizeMillis)
        }

        val pointMap = filteredPoints.associateBy { it.bucketStartUtc }
        val filledPoints = mutableListOf<UsageValuePoint>()
        var cursor = period.startUtc

        while (cursor < period.endUtc) {
            filledPoints += pointMap[cursor] ?: UsageValuePoint(
                bucketStartUtc = cursor,
                totalUsageMillis = 0L,
                totalLaunchCount = 0
            )
            cursor += bucketSizeMillis
        }
        return filledPoints
    }


    /**
     * Generates an empty series of usage points for the given UTC time range.
     * All timestamps remain in UTC - conversion to local time happens only at UI layer.
     */
    private fun generateEmptySeries(
        startUtcMillis: Long,
        endUtcMillis: Long,
        bucketSizeMillis: Long
    ): List<UsageValuePoint> {
        val result = mutableListOf<UsageValuePoint>()
        var cursor = startUtcMillis

        while (cursor < endUtcMillis) {
            result += UsageValuePoint(
                bucketStartUtc = cursor,
                totalUsageMillis = 0L,
                totalLaunchCount = 0
            )
            cursor += bucketSizeMillis
        }
        return result
    }

}