package com.librefocus.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.librefocus.data.repository.UsageTrackingRepository
import com.librefocus.models.AppUsageData
import com.librefocus.models.UsageValuePoint
import com.librefocus.utils.DateTimeFormatterManager
import com.librefocus.utils.FormattedDateTimePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class StatsViewModel(
    private val usageRepository: UsageTrackingRepository,
    private val dateTimeFormatterManager: DateTimeFormatterManager
) : ViewModel() {

    private val _metric = MutableStateFlow(StatsMetric.ScreenTime)
    val metric: StateFlow<StatsMetric> = _metric

    private val _range = MutableStateFlow(StatsRange.Day)
    val range: StateFlow<StatsRange> = _range
    
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

    private val _uiState = MutableStateFlow(StatsUiState(isLoading = true))
    val uiState: StateFlow<StatsUiState> = _uiState

    private var customRange: StatsPeriodState? = null

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
            runCatching {
                val period = _periodState.value  // Now guaranteed non-null
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
                val usagePointsLocal = convertUsagePointsToLocal(rawUsagePoints)
                val filledUsagePoints = fillMissingUsagePoints(usagePointsLocal, period, _range.value)
                
                val appUsage = usageRepository.getAppUsageSummaryInTimeRange(
                    startUtc = period.startUtc,
                    endUtc = period.endUtc
                )

                val rawUsageByHour = usageRepository.getUsageTotalsGroupedByHour(
                    startUtc = period.startUtc,
                    endUtc = period.endUtc
                )

                val usagePointsByDay = usageRepository.getUsageTotalsGroupedByDay(
                    startUtc = period.startUtc,
                    endUtc = period.endUtc
                )
                
                val totalUsageMillis = filledUsagePoints.sumOf { it.totalUsageMillis }
                val totalUnlocks = usageRepository.getDailyUnlockSummary(
                    startUtc = period.startUtc,
                    endUtc = period.endUtc
                ).sumOf { it.totalUnlocks }

                val insights = buildPhaseOneInsights(
                    period = period,
                    usageByDay = usagePointsByDay,
                    usageByHour = rawUsageByHour,
                    appUsage = appUsage,
                    totalUsageMillis = totalUsageMillis,
                    totalUnlocks = totalUnlocks
                )

                val phaseTwoInsights = buildPhaseTwoInsights(
                    period = period
                )
                
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
                    StatsMetric.Unlocks -> totalValue.toString()
                }
                
                val averageDisplayValue = when (metric) {
                    StatsMetric.ScreenTime -> formatDuration(averageValue)
                    StatsMetric.Opens -> averageValue.toString()
                    StatsMetric.Unlocks -> averageValue.toString()
                }
                
                val totalDisplayLabel = formatTotalLabel(metric)
                val averageDisplayLabel = formatAverageLabel(_range.value, metric)
                
                StatsUiState(
                    selectedRangeLabel = selectedLabel,
                    totalUsageMillis = totalUsageMillis,
                    totalUnlocks = totalUnlocks,
                    averageSessionMillis = averageSessionMillis,
                    usagePoints = filledUsagePoints,
                    appUsage = transformAppUsage(appUsage, metric),
                    isLoading = false,
                    totalDisplayValue = totalDisplayValue,
                    totalDisplayLabel = totalDisplayLabel,
                    averageDisplayValue = averageDisplayValue,
                    averageDisplayLabel = averageDisplayLabel,
                    phaseOneInsights = insights,
                    phaseTwoInsights = phaseTwoInsights
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
            StatsMetric.Unlocks -> appUsage // No per-app unlock data, keep by screen time
        }
    }

    /**
     * Creates an initial period state using system defaults.
     * This ensures the period state is never null, even before preferences load.
     */
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
            label = "Today"  // Default label, will be updated when preferences load
        )
    }
    
    private fun initialPeriodState(formatted: FormattedDateTimePreferences): StatsPeriodState {
        val nowLocal = ZonedDateTime.now(formatted.zoneId)
        return periodForDay(nowLocal, formatted)
    }

    /**
     * Creates a period state for a specific day in the user's local timezone.
     * @param localDateTime ZonedDateTime in the user's timezone
     * @param formatted Formatted date/time preferences
     */
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

    /**
     * Creates a period state for a specific date in the user's local timezone.
     * @param localDate LocalDate in the user's timezone
     * @param formatted Formatted date/time preferences
     */
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

    /**
     * Creates a period state for a week ending on the given day in the user's local timezone.
     * @param localDateTime ZonedDateTime in the user's timezone
     * @param formatted Formatted date/time preferences
     */
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

    /**
     * Creates a period state for a month in the user's local timezone.
     * @param localDateTime ZonedDateTime in the user's timezone
     * @param formatted Formatted date/time preferences
     */
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

    /**
     * Formats a date range label for display.
     * @param startUtc UTC timestamp of range start
     * @param durationMillis Duration of the range in milliseconds
     * @param formatted Formatted date/time preferences
     */
    private fun formatRangeLabel(startUtc: Long, durationMillis: Long, formatted: FormattedDateTimePreferences): String {
        return formatted.formatDateRange(startUtc, startUtc + durationMillis)
    }

    /**
     * Formats a custom date range label for display.
     * @param startUtc UTC timestamp of range start
     * @param endUtc UTC timestamp of range end (exclusive)
     * @param formatted Formatted date/time preferences
     */
    private fun formatCustomLabel(startUtc: Long, endUtc: Long, formatted: FormattedDateTimePreferences): String {
        return formatted.formatDateRange(startUtc, endUtc)
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
        val formatted = formattedPreferences.value ?: return points
        
        val bucketSizeMillis = when (range) {
            StatsRange.Day -> TimeUnit.HOURS.toMillis(1)
            StatsRange.Week, StatsRange.Month, StatsRange.Custom -> TimeUnit.DAYS.toMillis(1)
        }

        // Convert period boundaries to local time for bucket generation
        val startLocal = Instant.ofEpochMilli(period.startUtc).atZone(formatted.zoneId)
        val endLocal = Instant.ofEpochMilli(period.endUtc).atZone(formatted.zoneId)
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
     * Converts UTC-based usage points to local timezone.
     * The bucketStartUtc in each point is converted to the equivalent local time bucket.
     */
    private fun convertUsagePointsToLocal(utcPoints: List<UsageValuePoint>): List<UsageValuePoint> {
        val formatted = formattedPreferences.value ?: return utcPoints
        
        return utcPoints.map { point ->
            // Convert UTC bucket start to local time
            val utcInstant = Instant.ofEpochMilli(point.bucketStartUtc)
            val localDateTime = java.time.LocalDateTime.ofInstant(utcInstant, java.time.ZoneId.of("UTC"))
            val localInstant = localDateTime.atZone(formatted.zoneId).toInstant()

            point.copy(bucketStartUtc = localInstant.toEpochMilli())
        }
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

    private suspend fun buildPhaseOneInsights(
        period: StatsPeriodState,
        usageByDay: List<UsageValuePoint>,
        usageByHour: List<UsageValuePoint>,
        appUsage: List<AppUsageData>,
        totalUsageMillis: Long,
        totalUnlocks: Int
    ): PhaseOneInsights {
        val rangeDurationMillis = period.endUtc - period.startUtc
        val previousStartUtc = period.startUtc - rangeDurationMillis
        val previousEndUtc = period.startUtc

        val previousUsageByDay = usageRepository.getUsageTotalsGroupedByDay(
            startUtc = previousStartUtc,
            endUtc = previousEndUtc
        )
        val previousUnlocks = usageRepository.getDailyUnlockSummary(
            startUtc = previousStartUtc,
            endUtc = previousEndUtc
        )

        val currentScreenTime = usageByDay.sumOf { it.totalUsageMillis }
        val currentOpens = usageByDay.sumOf { it.totalLaunchCount }.toLong()
        val currentUnlocks = totalUnlocks.toLong()

        val previousScreenTime = previousUsageByDay.sumOf { it.totalUsageMillis }
        val previousOpens = previousUsageByDay.sumOf { it.totalLaunchCount }.toLong()
        val previousUnlocksTotal = previousUnlocks.sumOf { it.totalUnlocks }.toLong()

        val hasPreviousUsage = previousUsageByDay.isNotEmpty() || previousUnlocks.isNotEmpty()

        val comparison = ComparisonInsight(
            screenTime = buildMetricComparison(currentScreenTime, previousScreenTime, hasPreviousUsage),
            opens = buildMetricComparison(currentOpens, previousOpens, hasPreviousUsage),
            unlocks = buildMetricComparison(currentUnlocks, previousUnlocksTotal, hasPreviousUsage),
            mostUsedDayUtc = usageByDay.maxByOrNull { it.totalUsageMillis }?.bucketStartUtc
        )

        val zoneId = formattedPreferences.value?.zoneId ?: ZoneId.systemDefault()
        val usageByLocalHour = mutableMapOf<Int, Long>()
        usageByHour.forEach { point ->
            val hour = Instant.ofEpochMilli(point.bucketStartUtc).atZone(zoneId).hour
            usageByLocalHour[hour] = (usageByLocalHour[hour] ?: 0L) + point.totalUsageMillis
        }
        val sortedHours = usageByLocalHour.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }
        val lateNightMillis = usageByLocalHour
            .filterKeys { it >= 22 }
            .values
            .sum()
        val lateNightPercent = if (totalUsageMillis > 0L) {
            ((lateNightMillis.toDouble() * 100.0) / totalUsageMillis.toDouble()).roundToInt()
        } else {
            0
        }
        val peakHours = PeakHoursInsight(
            topHours = sortedHours,
            lateNightPercentage = lateNightPercent
        )

        val totalLaunches = usageByDay.sumOf { it.totalLaunchCount }
        val minutesPerUnlock = if (totalUnlocks > 0) {
            (totalUsageMillis.toDouble() / TimeUnit.MINUTES.toMillis(1).toDouble()) / totalUnlocks.toDouble()
        } else {
            null
        }
        val minutesPerLaunch = if (totalLaunches > 0) {
            (totalUsageMillis.toDouble() / TimeUnit.MINUTES.toMillis(1).toDouble()) / totalLaunches.toDouble()
        } else {
            null
        }

        val daysInRange = ((rangeDurationMillis / TimeUnit.DAYS.toMillis(1)).coerceAtLeast(1L)).toDouble()
        val currentUnlocksPerDay = totalUnlocks.toDouble() / daysInRange
        val checkingHeavy = calculateCheckingHeavyFlag(
            lookbackEndUtc = period.endUtc,
            currentUnlocksPerDay = currentUnlocksPerDay,
            currentMinutesPerUnlock = minutesPerUnlock
        )
        val unlockEfficiency = UnlockEfficiencyInsight(
            minutesPerUnlock = minutesPerUnlock,
            minutesPerLaunch = minutesPerLaunch,
            checkingHeavy = checkingHeavy
        )

        val totalAppTime = appUsage.sumOf { it.usageDurationMillis }
        val sortedAppTimes = appUsage.map { it.usageDurationMillis }.sortedDescending()
        fun sharePercent(topN: Int): Int {
            if (totalAppTime <= 0L) return 0
            val top = sortedAppTimes.take(topN).sum()
            return ((top.toDouble() * 100.0) / totalAppTime.toDouble()).roundToInt()
        }
        val concentration = ConcentrationInsight(
            top1Percentage = sharePercent(1),
            top3Percentage = sharePercent(3),
            top5Percentage = sharePercent(5)
        )

        return PhaseOneInsights(
            comparison = comparison,
            peakHours = peakHours,
            unlockEfficiency = unlockEfficiency,
            concentration = concentration
        )
    }

    private fun buildMetricComparison(
        currentValue: Long,
        previousValue: Long,
        hasPreviousData: Boolean
    ): MetricComparison {
        if (!hasPreviousData) {
            return MetricComparison(
                currentValue = currentValue,
                previousValue = null,
                deltaValue = null,
                deltaPercent = null
            )
        }

        val delta = currentValue - previousValue
        val percent = if (previousValue > 0L) {
            (delta.toDouble() * 100.0) / previousValue.toDouble()
        } else {
            null
        }
        return MetricComparison(
            currentValue = currentValue,
            previousValue = previousValue,
            deltaValue = delta,
            deltaPercent = percent
        )
    }

    private suspend fun calculateCheckingHeavyFlag(
        lookbackEndUtc: Long,
        currentUnlocksPerDay: Double,
        currentMinutesPerUnlock: Double?
    ): Boolean {
        if (currentMinutesPerUnlock == null) return false

        val lookbackStartUtc = lookbackEndUtc - TimeUnit.DAYS.toMillis(14)
        val lookbackUsageByDay = usageRepository.getUsageTotalsGroupedByDay(
            startUtc = lookbackStartUtc,
            endUtc = lookbackEndUtc
        )
        val lookbackUnlocks = usageRepository.getDailyUnlockSummary(
            startUtc = lookbackStartUtc,
            endUtc = lookbackEndUtc
        )

        if (lookbackUnlocks.size < 4) return false

        val usageByDayKey = lookbackUsageByDay.associateBy { it.bucketStartUtc }
        val unlockCounts = lookbackUnlocks.map { it.totalUnlocks.toDouble() }
        val minutesPerUnlockByDay = lookbackUnlocks.mapNotNull { unlockDay ->
            val usage = usageByDayKey[unlockDay.dateUtc]?.totalUsageMillis ?: 0L
            if (unlockDay.totalUnlocks > 0) {
                (usage.toDouble() / TimeUnit.MINUTES.toMillis(1).toDouble()) / unlockDay.totalUnlocks.toDouble()
            } else {
                null
            }
        }

        if (minutesPerUnlockByDay.size < 4) return false

        val unlockP75 = percentile(unlockCounts, 0.75)
        val minutesPerUnlockP25 = percentile(minutesPerUnlockByDay, 0.25)

        return currentUnlocksPerDay >= unlockP75 && currentMinutesPerUnlock <= minutesPerUnlockP25
    }

    private fun percentile(values: List<Double>, percentile: Double): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val index = ((sorted.size - 1) * percentile).toInt().coerceIn(0, sorted.lastIndex)
        return sorted[index]
    }

    private suspend fun buildPhaseTwoInsights(
        period: StatsPeriodState
    ): PhaseTwoInsights {
        val zoneId = formattedPreferences.value?.zoneId ?: ZoneId.systemDefault()
        val heatmapWindowDays = 28L
        val heatmapStartUtc = period.endUtc - TimeUnit.DAYS.toMillis(heatmapWindowDays)

        val rawEntries = usageRepository.getHourlyUsageEntriesInTimeRange(
            startUtc = heatmapStartUtc,
            endUtc = period.endUtc
        )

        val heatmapBuckets = mutableMapOf<Pair<Int, Int>, Long>()
        rawEntries.forEach { entry ->
            val local = Instant.ofEpochMilli(entry.hourStartUtc).atZone(zoneId)
            val weekday = local.dayOfWeek.value // Monday=1
            val hour = local.hour
            val key = weekday to hour
            heatmapBuckets[key] = (heatmapBuckets[key] ?: 0L) + entry.usageDurationMillis
        }

        val maxUsageMillis = heatmapBuckets.values.maxOrNull() ?: 0L
        val heatmapCells = buildList {
            for (weekday in 1..7) {
                for (hour in 0..23) {
                    val usageMillis = heatmapBuckets[weekday to hour] ?: 0L
                    val intensity = if (maxUsageMillis > 0L) {
                        (usageMillis.toFloat() / maxUsageMillis.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    add(
                        HeatmapCell(
                            weekday = weekday,
                            hour = hour,
                            usageMinutes = usageMillis.toDouble() / TimeUnit.MINUTES.toMillis(1).toDouble(),
                            intensity = intensity
                        )
                    )
                }
            }
        }

        val peakCell = heatmapBuckets.maxByOrNull { it.value }?.key
        val heatmap = UsageHeatmapInsight(
            cells = heatmapCells,
            peakWeekday = peakCell?.first,
            peakHour = peakCell?.second,
            windowDays = heatmapWindowDays.toInt()
        )

        val consistencyWindowDays = 30L
        val consistencyWindowStartUtc = period.endUtc - TimeUnit.DAYS.toMillis(consistencyWindowDays)
        val consistencyWindowDaily = usageRepository.getUsageTotalsGroupedByDay(
            startUtc = consistencyWindowStartUtc,
            endUtc = period.endUtc
        )
        val consistencyDailyMinutes = consistencyWindowDaily
            .sortedBy { it.bucketStartUtc }
            .map { day ->
                day.totalUsageMillis.toDouble() / TimeUnit.MINUTES.toMillis(1).toDouble()
            }
        val observedConsistencyDays = consistencyDailyMinutes.size

        val baselineMinutes = if (consistencyDailyMinutes.isNotEmpty()) {
            consistencyDailyMinutes.average().roundToInt()
        } else {
            0
        }

        val mean = consistencyDailyMinutes.average().takeIf { !it.isNaN() } ?: 0.0
        val stdDev = if (consistencyDailyMinutes.size > 1) {
            val variance = consistencyDailyMinutes.sumOf { value ->
                val diff = value - mean
                diff * diff
            } / consistencyDailyMinutes.size.toDouble()
            kotlin.math.sqrt(variance)
        } else {
            0.0
        }

        val cv = if (mean > 0.0) stdDev / mean else 0.0
        val consistencyScore = if (observedConsistencyDays >= 3) {
            ((1.0 - cv.coerceIn(0.0, 1.0)) * 100.0).roundToInt()
        } else {
            0
        }

        val baselineMedianMinutes = run {
            val values = consistencyDailyMinutes
            if (values.isEmpty()) 0.0 else percentile(values, 0.5)
        }

        val controlledThreshold = max(1, baselineMedianMinutes.roundToInt())
        val todayLocal = LocalDate.now(zoneId)

        // Compute streak from a rolling lookback window so it does not depend on selected range length.
        val streakLookbackStartUtc = period.endUtc - TimeUnit.DAYS.toMillis(90)
        val streakDaily = usageRepository.getUsageTotalsGroupedByDay(
            startUtc = streakLookbackStartUtc,
            endUtc = period.endUtc
        ).associateBy { it.bucketStartUtc }

        var streak = 0
        val latestRecordedDayUtc = streakDaily.keys.maxOrNull()
        var cursorDayUtc = min(
            period.endUtc - TimeUnit.DAYS.toMillis(1),
            latestRecordedDayUtc ?: (period.endUtc - TimeUnit.DAYS.toMillis(1))
        )
        while (cursorDayUtc >= streakLookbackStartUtc) {
            val localDay = Instant.ofEpochMilli(cursorDayUtc).atZone(zoneId).toLocalDate()
            if (localDay.isAfter(todayLocal)) {
                cursorDayUtc -= TimeUnit.DAYS.toMillis(1)
                continue
            }

            val dayUsage = streakDaily[cursorDayUtc] ?: break
            val usageMinutes = (dayUsage.totalUsageMillis
                .toDouble() / TimeUnit.MINUTES.toMillis(1).toDouble())

            if (usageMinutes <= controlledThreshold.toDouble()) {
                streak += 1
                cursorDayUtc -= TimeUnit.DAYS.toMillis(1)
            } else {
                break
            }
        }

        val streaks = StreaksConsistencyInsight(
            controlledDaysStreak = streak,
            consistencyScore = consistencyScore,
            volatilityMinutes = stdDev.roundToInt(),
            baselineMinutes = baselineMinutes
        )

        fun distinctByDay(entries: List<com.librefocus.data.local.database.entity.HourlyAppUsageEntity>): Map<Long, Int> {
            val byDay = mutableMapOf<Long, MutableSet<Int>>()
            entries.forEach { entry ->
                val dayStartLocal = Instant.ofEpochMilli(entry.hourStartUtc)
                    .atZone(zoneId)
                    .toLocalDate()
                    .atStartOfDay(zoneId)
                    .toInstant()
                    .toEpochMilli()
                val set = byDay.getOrPut(dayStartLocal) { mutableSetOf() }
                if (entry.usageDurationMillis > 0L) {
                    set += entry.appId
                }
            }
            return byDay.mapValues { it.value.size }
        }

        val sprawlWindowDays = 14L
        val currentSprawlStartUtc = period.endUtc - TimeUnit.DAYS.toMillis(sprawlWindowDays)
        val previousSprawlStartUtc = currentSprawlStartUtc - TimeUnit.DAYS.toMillis(sprawlWindowDays)

        val currentSprawlEntries = usageRepository.getHourlyUsageEntriesInTimeRange(
            startUtc = currentSprawlStartUtc,
            endUtc = period.endUtc
        )
        val previousSprawlEntries = usageRepository.getHourlyUsageEntriesInTimeRange(
            startUtc = previousSprawlStartUtc,
            endUtc = currentSprawlStartUtc
        )

        val currentDistinctByDay = distinctByDay(currentSprawlEntries)
        val previousDistinctByDay = distinctByDay(previousSprawlEntries)

        val currentDaysCount = max(1, currentDistinctByDay.size)
        val previousDaysCount = max(1, previousDistinctByDay.size)
        val currentAvgDistinct = currentDistinctByDay.values.sum().toDouble() / currentDaysCount.toDouble()
        val previousAvgDistinct = if (previousDistinctByDay.isNotEmpty()) {
            previousDistinctByDay.values.sum().toDouble() / previousDaysCount.toDouble()
        } else {
            null
        }
        val deltaPercent = if (previousAvgDistinct != null && previousAvgDistinct > 0.0) {
            (((currentAvgDistinct - previousAvgDistinct) * 100.0) / previousAvgDistinct).roundToInt()
        } else {
            null
        }

        val appSprawl = AppSprawlInsight(
            avgDistinctAppsPerDay = currentAvgDistinct,
            previousAvgDistinctAppsPerDay = previousAvgDistinct,
            deltaPercent = deltaPercent
        )

        return PhaseTwoInsights(
            heatmap = heatmap,
            streaks = streaks,
            appSprawl = appSprawl
        )
    }
}
