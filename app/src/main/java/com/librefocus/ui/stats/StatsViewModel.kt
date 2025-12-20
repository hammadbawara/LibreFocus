package com.librefocus.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.librefocus.data.repository.PreferencesRepository
import com.librefocus.data.repository.UsageTrackingRepository
import com.librefocus.models.AppUsageData
import com.librefocus.models.UsageValuePoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class StatsViewModel(
    private val usageRepository: UsageTrackingRepository,
) : ViewModel() {

    private val _metric = MutableStateFlow(StatsMetric.ScreenTime)
    val metric: StateFlow<StatsMetric> = _metric

    private val _range = MutableStateFlow(StatsRange.Day)
    val range: StateFlow<StatsRange> = _range

    // User's current timezone for converting UTC data to local time
    private val currentZone: ZoneId = ZoneId.systemDefault()

    // Formatters using user's local timezone
    private val dayLabelFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEE, dd MMM").withZone(currentZone)

    private val shortDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd MMM").withZone(currentZone)

    private val monthLabelFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMMM yyyy").withZone(currentZone)

    private val _periodState = MutableStateFlow(initialPeriodState())
    val periodState: StateFlow<StatsPeriodState> = _periodState

    private val _uiState = MutableStateFlow(StatsUiState(isLoading = true))
    val uiState: StateFlow<StatsUiState> = _uiState

    private var customRange: StatsPeriodState? = null

    init {
        refreshData()
    }

    fun onMetricSelected(newMetric: StatsMetric) {
        if (newMetric != _metric.value) {
            _metric.value = newMetric
            refreshData()
        }
    }

    fun onRangeSelected(range: StatsRange) {
        if (range == _range.value && range != StatsRange.Custom) return
        _range.value = range
        val nowLocal = ZonedDateTime.now(currentZone)
        val newPeriod = when (range) {
            StatsRange.Day -> periodForDay(nowLocal)
            StatsRange.Week -> periodForWeek(nowLocal)
            StatsRange.Month -> periodForMonth(nowLocal)
            StatsRange.Custom -> customRange ?: periodForWeek(nowLocal)
        }
        _periodState.value = newPeriod
        if (range == StatsRange.Custom) {
            customRange = newPeriod
        }
        refreshData()
    }

    fun onNavigatePrevious() {
        val current = _periodState.value
        val durationMillis = current.endUtc - current.startUtc
        val newPeriod = when (_range.value) {
            StatsRange.Day -> {
                val previousDay = localDateFromUtc(current.startUtc).minusDays(1)
                periodForDate(previousDay)
            }
            StatsRange.Week -> {
                val newStartUtc = current.startUtc - TimeUnit.DAYS.toMillis(7)
                val newEndUtc = current.endUtc - TimeUnit.DAYS.toMillis(7)
                StatsPeriodState(
                    startUtc = newStartUtc,
                    endUtc = newEndUtc,
                    label = formatRangeLabel(newStartUtc, durationMillis)
                )
            }
            StatsRange.Month -> {
                val previousMonth = localDateFromUtc(current.startUtc).minusMonths(1)
                periodForMonth(previousMonth.atStartOfDay(currentZone))
            }
            StatsRange.Custom -> customRange?.let {
                val length = it.endUtc - it.startUtc
                val newStart = current.startUtc - length
                StatsPeriodState(
                    startUtc = newStart,
                    endUtc = newStart + length,
                    label = formatRangeLabel(newStart, length)
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
        val current = _periodState.value
        val durationMillis = current.endUtc - current.startUtc
        val todayLocal = LocalDate.now(currentZone)
        val todayStartUtc = todayLocal.atStartOfDay(currentZone).toInstant().toEpochMilli()
        
        val newPeriod = when (_range.value) {
            StatsRange.Day -> {
                val nextDay = localDateFromUtc(current.startUtc).plusDays(1)
                val nextDayStartUtc = nextDay.atStartOfDay(currentZone).toInstant().toEpochMilli()
                if (nextDayStartUtc > todayStartUtc) return
                periodForDate(nextDay)
            }
            StatsRange.Week -> {
                val candidateStartUtc = current.startUtc + TimeUnit.DAYS.toMillis(7)
                if (candidateStartUtc >= todayStartUtc + TimeUnit.DAYS.toMillis(1)) return
                StatsPeriodState(
                    startUtc = candidateStartUtc,
                    endUtc = candidateStartUtc + durationMillis,
                    label = formatRangeLabel(candidateStartUtc, durationMillis)
                )
            }
            StatsRange.Month -> {
                val nextMonth = localDateFromUtc(current.startUtc).plusMonths(1)
                val nextMonthStartUtc = nextMonth.atStartOfDay(currentZone).toInstant().toEpochMilli()
                if (nextMonthStartUtc >= todayStartUtc + TimeUnit.DAYS.toMillis(1)) return
                periodForMonth(nextMonth.atStartOfDay(currentZone))
            }
            StatsRange.Custom -> {
                val length = current.endUtc - current.startUtc
                val candidateStart = current.startUtc + length
                if (candidateStart >= todayStartUtc + TimeUnit.DAYS.toMillis(1)) return
                val custom = StatsPeriodState(
                    startUtc = candidateStart,
                    endUtc = candidateStart + length,
                    label = formatRangeLabel(candidateStart, length)
                )
                customRange = custom
                custom
            }
        }
        _periodState.value = newPeriod
        refreshData()
    }

    fun onCustomRangeSelected(startLocalMillis: Long, endLocalMillis: Long) {
        // Convert local timestamps to UTC for storage and querying
        val startLocalDate = localDateFromUtc(startLocalMillis)
        val endLocalDate = localDateFromUtc(endLocalMillis)
        
        val startUtc = startLocalDate.atStartOfDay(currentZone).toInstant().toEpochMilli()
        val endUtc = endLocalDate.plusDays(1).atStartOfDay(currentZone).toInstant().toEpochMilli()
        
        if (endUtc <= startUtc) {
            _uiState.update { it.copy(errorMessage = "Invalid range selection.") }
            return
        }
        
        val label = formatCustomLabel(startUtc, endUtc)
        val period = StatsPeriodState(startUtc, endUtc, label)
        customRange = period
        _range.value = StatsRange.Custom
        _periodState.value = period
        refreshData()
    }

    private fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val period = _periodState.value
                val metric = _metric.value
                
                // Query data from repository using UTC timestamps
                val rawUsagePoints = when (_range.value) {
                    StatsRange.Day -> usageRepository.getUsageTotalsGroupedByHour(
                        period.startUtc,
                        period.endUtc
                    )
                    StatsRange.Week, StatsRange.Month, StatsRange.Custom -> usageRepository.getUsageTotalsGroupedByDay(
                        period.startUtc,
                        period.endUtc
                    )
                }
                
                // Convert UTC data points to local time for UI display
                ///val usagePointsLocal = convertUsagePointsToLocal(rawUsagePoints)
                val filledUsagePoints = fillMissingUsagePoints(rawUsagePoints, period, _range.value)
                
                val appUsage = usageRepository.getAppUsageSummaryInTimeRange(
                    startUtc = period.startUtc,
                    endUtc = period.endUtc
                )
                
                val totalUsageMillis = filledUsagePoints.sumOf { it.totalUsageMillis }
                val totalUnlocks = usageRepository.getDailyUnlockSummary(
                    startUtc = period.startUtc,
                    endUtc = period.endUtc
                ).sumOf { it.totalUnlocks }
                
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
                
                StatsUiState(
                    selectedRangeLabel = selectedLabel,
                    totalUsageMillis = totalUsageMillis,
                    totalUnlocks = totalUnlocks,
                    averageSessionMillis = averageSessionMillis,
                    usagePoints = filledUsagePoints,
                    appUsage = transformAppUsage(appUsage, metric),
                    isLoading = false
                )
            }.onSuccess { state ->
                _uiState.value = state
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLoading = false, errorMessage = throwable.message) }
            }
        }
    }

    private fun transformAppUsage(
        appUsage: List<AppUsageData>,
        metric: StatsMetric
    ): List<AppUsageData> {
        return when (metric) {
            StatsMetric.ScreenTime -> appUsage
            StatsMetric.Opens -> appUsage.sortedByDescending { it.launchCount }
        }
    }

    private fun initialPeriodState(): StatsPeriodState {
        val nowLocal = ZonedDateTime.now(currentZone)
        return periodForDay(nowLocal)
    }

    /**
     * Creates a period state for a specific day in the user's local timezone.
     * @param localDateTime ZonedDateTime in the user's timezone
     */
    private fun periodForDay(localDateTime: ZonedDateTime): StatsPeriodState {
        val dayStartLocal = localDateTime.toLocalDate().atStartOfDay(currentZone)
        val dayStartUtc = dayStartLocal.toInstant().toEpochMilli()
        val dayEndUtc = dayStartLocal.plusDays(1).toInstant().toEpochMilli()
        val label = dayLabelFormatter.format(dayStartLocal)
        return StatsPeriodState(
            startUtc = dayStartUtc,
            endUtc = dayEndUtc,
            label = label
        )
    }

    /**
     * Creates a period state for a specific date in the user's local timezone.
     * @param localDate LocalDate in the user's timezone
     */
    private fun periodForDate(localDate: LocalDate): StatsPeriodState {
        val dayStartLocal = localDate.atStartOfDay(currentZone)
        val dayStartUtc = dayStartLocal.toInstant().toEpochMilli()
        val dayEndUtc = dayStartLocal.plusDays(1).toInstant().toEpochMilli()
        val label = dayLabelFormatter.format(dayStartLocal)
        return StatsPeriodState(
            startUtc = dayStartUtc,
            endUtc = dayEndUtc,
            label = label
        )
    }

    /**
     * Creates a period state for a week ending on the given day in the user's local timezone.
     * @param localDateTime ZonedDateTime in the user's timezone
     */
    private fun periodForWeek(localDateTime: ZonedDateTime): StatsPeriodState {
        val dayStartLocal = localDateTime.toLocalDate().atStartOfDay(currentZone)
        val weekStartLocal = dayStartLocal.minusDays(6)
        val weekStartUtc = weekStartLocal.toInstant().toEpochMilli()
        val weekEndUtc = weekStartLocal.plusDays(7).toInstant().toEpochMilli()
        val label = formatRangeLabel(weekStartUtc, TimeUnit.DAYS.toMillis(7))
        return StatsPeriodState(
            startUtc = weekStartUtc,
            endUtc = weekEndUtc,
            label = label
        )
    }

    /**
     * Creates a period state for a month in the user's local timezone.
     * @param localDateTime ZonedDateTime in the user's timezone
     */
    private fun periodForMonth(localDateTime: ZonedDateTime): StatsPeriodState {
        val monthStartLocal = localDateTime.withDayOfMonth(1).toLocalDate().atStartOfDay(currentZone)
        val nextMonthStartLocal = monthStartLocal.plusMonths(1)
        val monthStartUtc = monthStartLocal.toInstant().toEpochMilli()
        val monthEndUtc = nextMonthStartLocal.toInstant().toEpochMilli()
        val label = monthLabelFormatter.format(monthStartLocal)
        return StatsPeriodState(
            startUtc = monthStartUtc,
            endUtc = monthEndUtc,
            label = label
        )
    }

    /**
     * Formats a date range label for display.
     * @param startUtc UTC timestamp of range start
     * @param durationMillis Duration of the range in milliseconds
     */
    private fun formatRangeLabel(startUtc: Long, durationMillis: Long): String {
        val startInstant = Instant.ofEpochMilli(startUtc).atZone(currentZone)
        val endInstant = Instant.ofEpochMilli(startUtc + durationMillis - 1).atZone(currentZone)
        val startLabel = shortDateFormatter.format(startInstant)
        val endLabel = shortDateFormatter.format(endInstant)
        return "$startLabel – $endLabel"
    }

    /**
     * Formats a custom date range label for display.
     * @param startUtc UTC timestamp of range start
     * @param endUtc UTC timestamp of range end (exclusive)
     */
    private fun formatCustomLabel(startUtc: Long, endUtc: Long): String {
        val startInstant = Instant.ofEpochMilli(startUtc).atZone(currentZone)
        val endInstant = Instant.ofEpochMilli(endUtc - 1).atZone(currentZone)
        val startLabel = shortDateFormatter.format(startInstant)
        val endLabel = shortDateFormatter.format(endInstant)
        return "$startLabel – $endLabel"
    }

    /**
     * Helper function to convert UTC milliseconds to LocalDate in user's timezone.
     */
    private fun localDateFromUtc(utcMillis: Long): LocalDate {
        return Instant.ofEpochMilli(utcMillis).atZone(currentZone).toLocalDate()
    }

    /**
     * Fills missing time buckets with empty data points.
     * @param points List of usage points in local time
     * @param period The period state containing UTC start and end times
     * @param range The stats range type
     */
    private fun fillMissingUsagePoints(
        points: List<UsageValuePoint>,
        period: StatsPeriodState,
        range: StatsRange
    ): List<UsageValuePoint> {
        val bucketSizeMillis = when (range) {
            StatsRange.Day -> TimeUnit.HOURS.toMillis(1)
            StatsRange.Week, StatsRange.Month, StatsRange.Custom -> TimeUnit.DAYS.toMillis(1)
        }

        // Convert period boundaries to local time for bucket generation
        val startLocal = Instant.ofEpochMilli(period.startUtc).atZone(currentZone)
        val endLocal = Instant.ofEpochMilli(period.endUtc).atZone(currentZone)
        val startLocalMillis = startLocal.toInstant().toEpochMilli()
        val endLocalMillis = endLocal.toInstant().toEpochMilli()

        val filteredPoints = points.filter { point ->
            point.bucketStartUtc >= startLocalMillis && point.bucketStartUtc < endLocalMillis
        }
        
        if (filteredPoints.isEmpty()) {
            return generateEmptySeriesLocal(startLocalMillis, endLocalMillis, range)
        }

        val pointMap = filteredPoints.associateBy { it.bucketStartUtc }
        val filledPoints = mutableListOf<UsageValuePoint>()
        var cursor = startLocalMillis
        
        while (cursor < endLocalMillis) {
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
     * Generates an empty series of usage points for the given local time range.
     */
    private fun generateEmptySeriesLocal(
        startLocalMillis: Long,
        endLocalMillis: Long,
        range: StatsRange
    ): List<UsageValuePoint> {
        val bucketSizeMillis = when (range) {
            StatsRange.Day -> TimeUnit.HOURS.toMillis(1)
            StatsRange.Week, StatsRange.Month, StatsRange.Custom -> TimeUnit.DAYS.toMillis(1)
        }
        val result = mutableListOf<UsageValuePoint>()
        var cursor = startLocalMillis
        
        while (cursor < endLocalMillis) {
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
