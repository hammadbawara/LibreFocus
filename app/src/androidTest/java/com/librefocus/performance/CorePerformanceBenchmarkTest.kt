package com.librefocus.performance

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.librefocus.ai.Anonymizer
import com.librefocus.models.DayOfWeek
import com.librefocus.models.TimeSlot
import com.librefocus.ui.limits.LimitSummaryFormatter
import com.librefocus.utils.extractUtcHourOfDay
import com.librefocus.utils.roundToDayStart
import com.librefocus.utils.roundToHourStart
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CorePerformanceBenchmarkTest {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun benchmarkComputeUserHash() {
        val key = ByteArray(32) { (it + 7).toByte() }
        val ids = List(500) { index -> "com.example.app.$index-user-id-$index" }

        benchmarkRule.measureRepeated {
            for (id in ids) {
                Anonymizer.computeUserHash(key, id)
            }
        }
    }

    @Test
    fun benchmarkValidateSummaryKeys() {
        val validMap = mapOf(
            "userHash" to "abc12345",
            "daysObserved" to 14,
            "dailyMeanMinutes" to 92.4,
            "rollingMean7" to 88.2,
            "trendSlopeMinutesPerDay" to -2.6,
            "goalMinutes" to 120,
            "goalAdherenceRate" to 0.85,
            "sessionCount" to 42,
            "avgSessionMinutes" to 24.7,
            "timeOfDayBuckets" to mapOf("morning" to 44, "afternoon" to 31),
            "noData" to false
        )

        benchmarkRule.measureRepeated {
            repeat(2_500) {
                Anonymizer.validateSummaryKeys(validMap)
            }
        }
    }

    @Test
    fun benchmarkFormatScheduleSummary() {
        val slots = listOf(
            TimeSlot(fromHour = 9 * 60, toHour = 12 * 60),
            TimeSlot(fromHour = 14 * 60 + 30, toHour = 18 * 60)
        )
        val workDays = setOf(DayOfWeek.MON, DayOfWeek.TUE, DayOfWeek.WED, DayOfWeek.THU, DayOfWeek.FRI)

        benchmarkRule.measureRepeated {
            repeat(1_500) {
                LimitSummaryFormatter.formatScheduleSummary(
                    isAllDay = false,
                    timeSlots = slots,
                    selectedDays = workDays
                )
            }
        }
    }

    @Test
    fun benchmarkUtcRoundingHelpers() {
        val samples = LongArray(2_000) { 1_710_000_000_000L + it * 59_321L }

        benchmarkRule.measureRepeated {
            for (ts in samples) {
                roundToHourStart(ts)
                roundToDayStart(ts)
                extractUtcHourOfDay(ts)
            }
        }
    }
}
