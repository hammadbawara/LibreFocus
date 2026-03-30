package com.librefocus.ai

import com.librefocus.data.local.database.dao.DailyDeviceUsageDao
import com.librefocus.data.local.database.dao.HourlyAppUsageDao
import com.librefocus.data.local.database.dao.AppDao
import com.librefocus.models.HourlyUsageData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.roundToInt

/**
 * Builds anonymized BehavioralSummary objects from DAOs.
 */
class ChatContextProvider(
    private val dailyDeviceUsageDao: DailyDeviceUsageDao,
    private val hourlyAppUsageDao: HourlyAppUsageDao,
    private val appDao: AppDao,
    private val appContext: android.content.Context
) : com.librefocus.ui.chatbot.IChatContextProvider {

    data class BehavioralSummary(
        val userHash: String,
        val daysObserved: Int,
        val dailyMeanMinutes: Double,
        val rollingMean7: Double,
        val trendSlopeMinutesPerDay: Double,
        val goalMinutes: Int,
        val goalAdherenceRate: Double,
        val sessionCount: Int,
        val avgSessionMinutes: Double,
        val timeOfDayBuckets: Map<String, Double>,
        val noData: Boolean = false
    )

    suspend fun buildSummary(windowStartMs: Long, windowEndMs: Long, rawUserId: String?, goalMinutes: Int?): BehavioralSummary =
        withContext(Dispatchers.IO) {
            // compute user hash
            val secret = Anonymizer.getOrCreateSecret(appContext)
            val userHash = if (rawUserId != null) Anonymizer.computeUserHash(secret, rawUserId) else "anon"

            // get hourly usage entries in window
            val hourly = hourlyAppUsageDao.getUsageInTimeRangeOnce(windowStartMs, windowEndMs)

            if (hourly.isEmpty()) {
                return@withContext BehavioralSummary(
                    userHash = userHash,
                    daysObserved = 0,
                    dailyMeanMinutes = 0.0,
                    rollingMean7 = 0.0,
                    trendSlopeMinutesPerDay = 0.0,
                    goalMinutes = goalMinutes ?: 0,
                    goalAdherenceRate = 0.0,
                    sessionCount = 0,
                    avgSessionMinutes = 0.0,
                    timeOfDayBuckets = mapOf("morning" to 0.0, "afternoon" to 0.0, "evening" to 0.0, "night" to 0.0),
                    noData = true
                )
            }

            // Aggregate hourly into daily totals
            val dailyMap = mutableMapOf<Long, Long>()
            var totalMillis = 0L
            var sessionCount = 0
            val buckets = mutableMapOf("morning" to 0L, "afternoon" to 0L, "evening" to 0L, "night" to 0L)

            hourly.forEach { h ->
                val dayStart = h.hourStartUtc.roundToDayStart()
                dailyMap[dayStart] = (dailyMap[dayStart] ?: 0L) + h.usageDurationMillis
                totalMillis += h.usageDurationMillis
                sessionCount += h.launchCount

                // time of day bucket
                val hour = h.hourStartUtc.extractUtcHourOfDay()
                val bucket = when (hour) {
                    in 5..10 -> "morning"
                    in 11..16 -> "afternoon"
                    in 17..21 -> "evening"
                    else -> "night"
                }
                buckets[bucket] = (buckets[bucket] ?: 0L) + h.usageDurationMillis
            }

            val daysObserved = dailyMap.size
            val dailyTotals = dailyMap.values.map { millis -> millis.toDouble() / 60000.0 }
            val dailyMean = if (dailyTotals.isNotEmpty()) dailyTotals.average() else 0.0

            // rolling mean 7 (use available days if <7)
            val rollingMean7 = if (dailyTotals.isNotEmpty()) {
                val last7 = dailyTotals.takeLast(7)
                last7.average()
            } else 0.0

            // compute slope via simple linear regression on dailyTotals
            val slope = computeSlope(dailyTotals)

            val avgSessionMinutes = if (sessionCount > 0) (totalMillis.toDouble() / sessionCount) / 60000.0 else 0.0

            val bucketsPercent = buckets.mapValues { (_, millis) ->
                if (totalMillis == 0L) 0.0 else (millis.toDouble() / totalMillis.toDouble()) * 100.0
            }

            val adherence = if (goalMinutes != null && goalMinutes > 0) {
                // proportion of days below goal
                val below = dailyTotals.count { it <= goalMinutes }
                below.toDouble() / daysObserved.toDouble()
            } else 0.0

            return@withContext BehavioralSummary(
                userHash = userHash,
                daysObserved = daysObserved,
                dailyMeanMinutes = dailyMean,
                rollingMean7 = rollingMean7,
                trendSlopeMinutesPerDay = slope,
                goalMinutes = goalMinutes ?: 0,
                goalAdherenceRate = adherence,
                sessionCount = sessionCount,
                avgSessionMinutes = avgSessionMinutes,
                timeOfDayBuckets = bucketsPercent,
                noData = false
            )
        }

    // Implement the legacy interface method expected by ChatViewModel
    override suspend fun getBehaviorContext(): String {
        // Build a concise human-readable summary for the chatbot using the last 24 hours
        val now = System.currentTimeMillis()
        val start = LocalDate.now().atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000
        val end = start + 24 * 60 * 60 * 1000 - 1

        val summary = buildSummary(start, end, rawUserId = null, goalMinutes = null)

        if (summary.noData) {
            return "USER'S CURRENT BEHAVIOR CONTEXT:\n- No recent usage data available."
        }

        val topBuckets = summary.timeOfDayBuckets.entries.sortedByDescending { it.value }
            .joinToString(", ") { "${it.key}: ${String.format("%.1f", it.value)}%" }

        return """
        USER'S CURRENT BEHAVIOR CONTEXT:
        - Days observed: ${summary.daysObserved}
        - Daily mean screen time: ${String.format("%.1f", summary.dailyMeanMinutes)} minutes
        - 7-day rolling mean: ${String.format("%.1f", summary.rollingMean7)} minutes
        - Trend (slope): ${String.format("%.2f", summary.trendSlopeMinutesPerDay)} minutes/day
        - Goal adherence rate: ${String.format("%.2f", summary.goalAdherenceRate)}
        - Avg session length: ${String.format("%.1f", summary.avgSessionMinutes)} minutes
        - Time of day distribution: $topBuckets
        """.trimIndent()
    }

    private fun Long.roundToDayStart(): Long {
        val DAY_MILLIS = 24 * 60 * 60 * 1000L
        return this - (this % DAY_MILLIS)
    }

    private fun Long.extractUtcHourOfDay(): Int {
        val DAY_MILLIS = 24 * 60 * 60 * 1000L
        val millisIntoDay = this - (this - (this % DAY_MILLIS))
        return ((millisIntoDay / (60 * 60 * 1000)) % 24).toInt()
    }

    private fun computeSlope(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val n = values.size
        val xMean = (0 until n).average()
        val yMean = values.average()
        var num = 0.0
        var den = 0.0
        for (i in 0 until n) {
            val x = i.toDouble()
            val y = values[i]
            num += (x - xMean) * (y - yMean)
            den += (x - xMean) * (x - xMean)
        }
        return if (den == 0.0) 0.0 else num / den
    }
}
