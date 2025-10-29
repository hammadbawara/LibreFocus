package com.librefocus.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.librefocus.data.repository.UsageTrackingRepository
import com.librefocus.models.AppUsageData
import com.librefocus.models.UsageValuePoint
import com.librefocus.utils.roundToDayStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class StatsViewModel(
    private val repository: UsageTrackingRepository
) : ViewModel() {

    private val _metric = MutableStateFlow(StatsMetric.ScreenTime)
    val metric: StateFlow<StatsMetric> = _metric

    private val _range = MutableStateFlow(StatsRange.Day)
    val range: StateFlow<StatsRange> = _range

    private val userZone: ZoneId = ZoneId.systemDefault()

    private val dayLabelFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEE, dd MMM").withZone(userZone)

    private val shortDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd MMM").withZone(userZone)

    private val monthLabelFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMMM yyyy").withZone(userZone)

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
        val now = System.currentTimeMillis()
        val newPeriod = when (range) {
            StatsRange.Day -> periodForDay(now)
            StatsRange.Week -> periodForWeek(now)
            StatsRange.Month -> periodForMonth(now)
            StatsRange.Custom -> customRange ?: periodForWeek(now)
        }
        _periodState.value = newPeriod
        if (range == StatsRange.Custom) {
            customRange = newPeriod
        }
        refreshData()
    }

    fun onNavigatePrevious() {
        val current = _periodState.value
        val diff = current.currentEndUtc - current.currentStartUtc
        val newPeriod = when (_range.value) {
            StatsRange.Day -> periodForDay(current.currentStartUtc - TimeUnit.DAYS.toMillis(1))
            StatsRange.Week -> StatsPeriodState(
                currentStartUtc = current.currentStartUtc - TimeUnit.DAYS.toMillis(7),
                currentEndUtc = current.currentEndUtc - TimeUnit.DAYS.toMillis(7),
                label = formatRangeLabel(current.currentStartUtc - TimeUnit.DAYS.toMillis(7), diff)
            )
            StatsRange.Month -> periodForMonth(current.currentStartUtc - TimeUnit.DAYS.toMillis(30))
            StatsRange.Custom -> customRange?.let {
                val length = it.currentEndUtc - it.currentStartUtc
                val newStart = current.currentStartUtc - length
                StatsPeriodState(
                    currentStartUtc = newStart,
                    currentEndUtc = newStart + length,
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
        val diff = current.currentEndUtc - current.currentStartUtc
        val now = System.currentTimeMillis()
        val newPeriod = when (_range.value) {
            StatsRange.Day -> {
                val candidateStart = current.currentStartUtc + TimeUnit.DAYS.toMillis(1)
                if (candidateStart > roundToDayStart(now)) return
                periodForDay(candidateStart)
            }
            StatsRange.Week -> {
                val candidateStart = current.currentStartUtc + TimeUnit.DAYS.toMillis(7)
                if (candidateStart >= roundToDayStart(now) + TimeUnit.DAYS.toMillis(1)) return
                StatsPeriodState(
                    currentStartUtc = candidateStart,
                    currentEndUtc = candidateStart + diff,
                    label = formatRangeLabel(candidateStart, diff)
                )
            }
            StatsRange.Month -> {
                val candidateStart = current.currentStartUtc + TimeUnit.DAYS.toMillis(30)
                if (candidateStart >= roundToDayStart(now) + TimeUnit.DAYS.toMillis(1)) return
                periodForMonth(candidateStart)
            }
            StatsRange.Custom -> {
                val length = current.currentEndUtc - current.currentStartUtc
                val candidateStart = current.currentStartUtc + length
                if (candidateStart >= roundToDayStart(now) + TimeUnit.DAYS.toMillis(1)) return
                val custom = StatsPeriodState(
                    currentStartUtc = candidateStart,
                    currentEndUtc = candidateStart + length,
                    label = formatRangeLabel(candidateStart, length)
                )
                customRange = custom
                custom
            }
        }
        _periodState.value = newPeriod
        refreshData()
    }

    fun onCustomRangeSelected(startUtc: Long, endUtc: Long) {
        val alignedStart = roundToDayStart(startUtc)
        val alignedEnd = roundToDayStart(endUtc) + TimeUnit.DAYS.toMillis(1)
        if (alignedEnd <= alignedStart) {
            _uiState.update { it.copy(errorMessage = "Invalid range selection.") }
            return
        }
        val label = formatCustomLabel(alignedStart, alignedEnd)
        val period = StatsPeriodState(alignedStart, alignedEnd, label)
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
                val rawUsagePoints = when (_range.value) {
                    StatsRange.Day -> repository.getUsageTotalsGroupedByHour(
                        period.currentStartUtc,
                        period.currentEndUtc
                    )
                    StatsRange.Week, StatsRange.Month, StatsRange.Custom -> repository.getUsageTotalsGroupedByDay(
                        period.currentStartUtc,
                        period.currentEndUtc
                    )
                }
                val usagePoints = fillMissingUsagePoints(rawUsagePoints, period, _range.value)
                val appUsage = repository.getAppUsageSummaryInTimeRange(
                    startUtc = period.currentStartUtc,
                    endUtc = period.currentEndUtc
                )
                val totalUsageMillis = usagePoints.sumOf { it.totalUsageMillis }
                val totalUnlocks = repository.getDailyUnlockSummary(
                    startUtc = period.currentStartUtc,
                    endUtc = period.currentEndUtc
                ).sumOf { it.totalUnlocks }
                val activeUsageBuckets = usagePoints.count { it.totalUsageMillis > 0 }
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
                    usagePoints = usagePoints,
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
        val now = System.currentTimeMillis()
        return periodForDay(now)
    }

    private fun periodForDay(referenceUtc: Long): StatsPeriodState {
        val dayStart = roundToDayStart(referenceUtc)
        val label = dayLabelFormatter.format(Instant.ofEpochMilli(dayStart))
        return StatsPeriodState(
            currentStartUtc = dayStart,
            currentEndUtc = dayStart + TimeUnit.DAYS.toMillis(1),
            label = label
        )
    }

    private fun periodForWeek(referenceUtc: Long): StatsPeriodState {
        val dayStart = roundToDayStart(referenceUtc)
        val weekStart = dayStart - TimeUnit.DAYS.toMillis(6)
        val label = formatRangeLabel(weekStart, TimeUnit.DAYS.toMillis(7))
        return StatsPeriodState(
            currentStartUtc = weekStart,
            currentEndUtc = weekStart + TimeUnit.DAYS.toMillis(7),
            label = label
        )
    }

    private fun periodForMonth(referenceUtc: Long): StatsPeriodState {
        val instant = Instant.ofEpochMilli(referenceUtc)
        val monthStart = instant.atZone(userZone).withDayOfMonth(1).toInstant().toEpochMilli()
        val nextMonthStart = instant.atZone(userZone).plusMonths(1).withDayOfMonth(1).toInstant().toEpochMilli()
        val label = monthLabelFormatter.format(Instant.ofEpochMilli(monthStart))
        return StatsPeriodState(
            currentStartUtc = monthStart,
            currentEndUtc = nextMonthStart,
            label = label
        )
    }

    private fun formatRangeLabel(startUtc: Long, durationMillis: Long): String {
        val startLabel = shortDateFormatter.format(Instant.ofEpochMilli(startUtc))
        val endLabel = shortDateFormatter.format(Instant.ofEpochMilli(startUtc + durationMillis - 1))
        return "$startLabel – $endLabel"
    }

    private fun formatCustomLabel(startUtc: Long, endUtc: Long): String {
        val startLabel = shortDateFormatter.format(Instant.ofEpochMilli(startUtc))
        val endLabel = shortDateFormatter.format(Instant.ofEpochMilli(endUtc - 1))
        return "$startLabel – $endLabel"
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

        val filteredPoints = points.filter { point ->
            point.bucketStartUtc >= period.currentStartUtc && point.bucketStartUtc < period.currentEndUtc
        }
        if (filteredPoints.isEmpty()) {
            return generateEmptySeries(period, range)
        }

        val pointMap = filteredPoints.associateBy { it.bucketStartUtc }
        val filledPoints = mutableListOf<UsageValuePoint>()
        var cursor = period.currentStartUtc
        while (cursor < period.currentEndUtc) {
            filledPoints += pointMap[cursor] ?: UsageValuePoint(
                bucketStartUtc = cursor,
                totalUsageMillis = 0L,
                totalLaunchCount = 0
            )
            cursor += bucketSizeMillis
        }
        return filledPoints
    }

    private fun generateEmptySeries(period: StatsPeriodState, range: StatsRange): List<UsageValuePoint> {
        val bucketSizeMillis = when (range) {
            StatsRange.Day -> TimeUnit.HOURS.toMillis(1)
            StatsRange.Week, StatsRange.Month, StatsRange.Custom -> TimeUnit.DAYS.toMillis(1)
        }
        val result = mutableListOf<UsageValuePoint>()
        var cursor = period.currentStartUtc
        while (cursor < period.currentEndUtc) {
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
