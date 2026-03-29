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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

class StatsViewModel(
    private val usageRepository: UsageTrackingRepository,
    private val dateTimeFormatterManager: DateTimeFormatterManager
) : ViewModel() {

    private var _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState

    val formattedPreferences: StateFlow<FormattedDateTimePreferences?> =
        dateTimeFormatterManager.formattedPreferences
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    suspend fun refreshUsageStats(period: StatsPeriodState, metric: StatsMetric = StatsMetric.ScreenTime, range: StatsRange = StatsRange.Day) {
        viewModelScope.launch {
            _uiState.update {it.copy(isLoading = true)}

            runCatching {
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

                val usagePointsLocal = convertUsagePointsToLocal(usagePointsByDay)
                val filledUsagePoints = fillMissingUsagePoints(usagePointsLocal, period, range)

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

                val phaseThreeInsights = buildPhaseThreeInsights(
                    period = period,
                    range = range
                )

                val activeUsageBuckets = filledUsagePoints.count { it.totalUsageMillis > 0 }
                val averageSessionMillis = if (activeUsageBuckets > 0) {
                    totalUsageMillis / activeUsageBuckets
                } else {
                    0L
                }
                
                val selectedLabel = period.label

                // Calculate total and average using utility functions
                val totalValue = calculateTotal(filledUsagePoints, metric)
                val averageValue = calculateAverage(filledUsagePoints, metric, range)
                
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
                val averageDisplayLabel = formatAverageLabel(range, metric)
                
                StatsUiState(
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
                    phaseTwoInsights = phaseTwoInsights,
                    phaseThreeInsights = phaseThreeInsights
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

    // ─────────────────────────────────────────────────────────────────────────
    // Phase 3: Predictions & Correlations
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun buildPhaseThreeInsights(
        period: StatsPeriodState,
        range: StatsRange
    ): PhaseThreeInsights {
        val zoneId = formattedPreferences.value?.zoneId ?: ZoneId.systemDefault()
        val todayStartUtc = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val isViewingToday = range == StatsRange.Day && period.startUtc == todayStartUtc
        val forecast = if (isViewingToday) buildForecastInsight(period) else null
        val correlation = buildCorrelationInsight(period)
        return PhaseThreeInsights(forecast = forecast, correlation = correlation)
    }

    /**
     * Builds an end-of-day forecast for today.
     * Projects remaining hours using per-hour medians from the same weekday over the last 4 weeks.
     * Only called when the selected Day period is the current calendar day.
     */
    private suspend fun buildForecastInsight(period: StatsPeriodState): EndOfDayForecastInsight {
        val zoneId = formattedPreferences.value?.zoneId ?: ZoneId.systemDefault()
        val currentHour = ZonedDateTime.now(zoneId).hour

        // Fetch actual usage for the day being viewed
        val todayEntries = usageRepository.getHourlyUsageEntriesInTimeRange(
            startUtc = period.startUtc,
            endUtc = period.endUtc
        )
        val actualUsageMillis = todayEntries.sumOf { it.usageDurationMillis }

        // Collect same-weekday data from the past 4 weeks
        val weekday = Instant.ofEpochMilli(period.startUtc).atZone(zoneId).toLocalDate().dayOfWeek
        val weekdayName = weekday.getDisplayName(TextStyle.FULL, Locale.getDefault())

        // Build per-hour maps for up to 4 past same-weekday occurrences
        val pastWeekHourMaps = mutableListOf<Map<Int, Long>>() // each map: hour -> millis
        for (weeksBack in 1..4) {
            val pastDayStart = period.startUtc - TimeUnit.DAYS.toMillis(7L * weeksBack)
            val pastDayEnd = pastDayStart + TimeUnit.DAYS.toMillis(1L)
            val pastEntries = usageRepository.getHourlyUsageEntriesInTimeRange(
                startUtc = pastDayStart,
                endUtc = pastDayEnd
            )
            if (pastEntries.isNotEmpty()) {
                val hourMap = mutableMapOf<Int, Long>()
                pastEntries.forEach { entry ->
                    val h = Instant.ofEpochMilli(entry.hourStartUtc).atZone(zoneId).hour
                    hourMap[h] = (hourMap[h] ?: 0L) + entry.usageDurationMillis
                }
                pastWeekHourMaps += hourMap
            }
        }

        val weeksUsed = pastWeekHourMaps.size

        // Typical full-day median for the same weekday
        val pastDayTotals = pastWeekHourMaps.map { it.values.sum() }.sorted()
        val typicalSameWeekdayMillis: Long? = if (pastDayTotals.isNotEmpty()) {
            val mid = pastDayTotals.size / 2
            if (pastDayTotals.size % 2 == 0) {
                (pastDayTotals[mid - 1] + pastDayTotals[mid]) / 2
            } else {
                pastDayTotals[mid]
            }
        } else null

        // Project remaining hours using per-hour medians from past same-weekday data
        val remainingForecastMillis: Long
        val forecastedTotalMillis: Long
        if (weeksUsed > 0) {
            var remainingSum = 0L
            for (h in (currentHour + 1)..23) {
                val hourValues = pastWeekHourMaps.mapNotNull { it[h] }.sorted()
                if (hourValues.isNotEmpty()) {
                    val mid = hourValues.size / 2
                    remainingSum += if (hourValues.size % 2 == 0) {
                        (hourValues[mid - 1] + hourValues[mid]) / 2
                    } else {
                        hourValues[mid]
                    }
                }
            }
            remainingForecastMillis = remainingSum
            forecastedTotalMillis = actualUsageMillis + remainingForecastMillis
        } else {
            remainingForecastMillis = 0L
            forecastedTotalMillis = actualUsageMillis
        }

        // Delta vs typical (in %)
        val typicalDeltaPercent: Int? = typicalSameWeekdayMillis?.let { typical ->
            if (typical > 0L) {
                (((forecastedTotalMillis - typical).toDouble() / typical.toDouble()) * 100.0).roundToInt()
            } else null
        }

        val confidence = when (weeksUsed) {
            0, 1 -> ForecastConfidence.LOW
            2, 3 -> ForecastConfidence.MEDIUM
            else -> ForecastConfidence.HIGH
        }

        return EndOfDayForecastInsight(
            actualUsageMillis = actualUsageMillis,
            forecastedTotalMillis = forecastedTotalMillis,
            remainingForecastMillis = remainingForecastMillis,
            typicalSameWeekdayMillis = typicalSameWeekdayMillis,
            typicalDeltaPercent = typicalDeltaPercent,
            currentHour = currentHour,
            weeksUsed = weeksUsed,
            confidence = confidence,
            weekdayName = weekdayName
        )
    }

    /**
     * Computes the Pearson correlation between daily unlock count and daily screen time
     * over a fixed 30-day rolling window ending at [period.endUtc].
     * Returns null when fewer than 7 paired days are available.
     */
    private suspend fun buildCorrelationInsight(period: StatsPeriodState): CorrelationInsight? {
        // Use the selected range, but extend back to at least 30 days to ensure meaningful correlation
        val lookbackStartUtc = minOf(period.startUtc, period.endUtc - TimeUnit.DAYS.toMillis(30L))
        val dailyUsage = usageRepository.getUsageTotalsGroupedByDay(
            startUtc = lookbackStartUtc,
            endUtc = period.endUtc
        )
        val dailyUnlocks = usageRepository.getDailyUnlockSummary(
            startUtc = lookbackStartUtc,
            endUtc = period.endUtc
        )

        // Join by day bucket key
        val usageByDay = dailyUsage.associateBy { it.bucketStartUtc }
        data class Pair(val unlocks: Double, val usageMinutes: Double)
        val pairs = dailyUnlocks.mapNotNull { unlock ->
            val usage = usageByDay[unlock.dateUtc] ?: return@mapNotNull null
            if (unlock.totalUnlocks <= 0 || usage.totalUsageMillis <= 0L) return@mapNotNull null
            Pair(
                unlocks = unlock.totalUnlocks.toDouble(),
                usageMinutes = usage.totalUsageMillis.toDouble() / TimeUnit.MINUTES.toMillis(1).toDouble()
            )
        }

        if (pairs.size < 3) return null

        val n = pairs.size.toDouble()
        val xs = pairs.map { it.unlocks }
        val ys = pairs.map { it.usageMinutes }
        val mx = xs.average()
        val my = ys.average()
        val stdX = sqrt(xs.sumOf { (it - mx) * (it - mx) } / n)
        val stdY = sqrt(ys.sumOf { (it - my) * (it - my) } / n)

        val r = if (stdX > 0.0 && stdY > 0.0) {
            pairs.sumOf { (it.unlocks - mx) * (it.usageMinutes - my) } / (n * stdX * stdY)
        } else {
            0.0
        }
        val rClamped = r.coerceIn(-1.0, 1.0)

        val strength = when {
            abs(rClamped) < 0.15 -> CorrelationStrength.NONE
            abs(rClamped) < 0.35 -> CorrelationStrength.WEAK
            abs(rClamped) < 0.60 -> CorrelationStrength.MODERATE
            else -> CorrelationStrength.STRONG
        }

        val message = when {
            rClamped > 0.35  -> CorrelationMessage.MORE_UNLOCKS_MORE_TIME
            rClamped < -0.25 -> CorrelationMessage.MORE_UNLOCKS_SHORTER_SESSIONS
            else             -> CorrelationMessage.UNLOCKS_NOT_PREDICTIVE
        }

        return CorrelationInsight(
            pearsonR = rClamped,
            strength = strength,
            dayCount = pairs.size,
            message = message
        )
    }
}
