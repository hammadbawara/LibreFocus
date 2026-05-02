package com.librefocus.data.repository

import com.librefocus.data.local.database.dao.AchievementDao
import com.librefocus.data.local.database.dao.GoalHistoryDao
import com.librefocus.data.local.database.dao.HourlyAppUsageDao
import com.librefocus.data.local.database.dao.PerfectDayDao
import com.librefocus.data.local.database.dao.SyncMetadataDao
import com.librefocus.data.local.database.entity.AchievementEntity
import com.librefocus.data.local.database.entity.GoalHistoryEntity
import com.librefocus.data.local.database.entity.PerfectDayEntity
import com.librefocus.data.local.database.entity.SyncMetadataEntity
import com.librefocus.models.AchievementAnnouncement
import com.librefocus.models.AchievementGroup
import com.librefocus.models.AchievementType
import com.librefocus.models.GamificationSnapshot
import com.librefocus.models.calculateLevelProgress
import com.librefocus.models.toRecord
import com.librefocus.models.xpEarned
import com.librefocus.utils.roundToDayStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

class GamificationRepository(
    private val preferencesRepository: PreferencesRepository,
    private val hourlyAppUsageDao: HourlyAppUsageDao,
    private val perfectDayDao: PerfectDayDao,
    private val achievementDao: AchievementDao,
    private val goalHistoryDao: GoalHistoryDao,
    private val syncMetadataDao: SyncMetadataDao
) {

    companion object {
        private const val LAST_CALCULATED_DATE_KEY = "gamification_last_calculated_date_utc"
        private const val BASE_CURRENT_STREAK_KEY = "gamification_base_current_streak"
        private const val BASE_LONGEST_STREAK_KEY = "gamification_base_longest_streak"
        private const val BASE_TOTAL_PERFECT_DAYS_KEY = "gamification_base_total_perfect_days"
        private const val LAST_SEEN_ACHIEVEMENT_AT_UTC_KEY = "gamification_last_seen_achievement_at_utc"
        private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
    }

    suspend fun setDailyScreenTimeGoal(minutes: Int): Result<GamificationSnapshot> = withContext(Dispatchers.IO) {
        try {
            val clampedMinutes = minutes.coerceAtLeast(0)
            val nowUtc = System.currentTimeMillis()
            val dayStartUtc = roundToDayStart(nowUtc)
            val currentGoal = goalHistoryDao.getActiveGoal()

            if (currentGoal == null) {
                goalHistoryDao.insertGoal(
                    GoalHistoryEntity(
                        goalMinutes = clampedMinutes,
                        startDateUtc = dayStartUtc,
                        endDateUtc = null,
                        createdAtUtc = nowUtc
                    )
                )
            } else if (currentGoal.startDateUtc == dayStartUtc && currentGoal.endDateUtc == null) {
                goalHistoryDao.updateGoalMinutes(currentGoal.id, clampedMinutes)
            } else if (currentGoal.goalMinutes != clampedMinutes || currentGoal.endDateUtc != null) {
                goalHistoryDao.closeGoal(currentGoal.id, dayStartUtc)
                goalHistoryDao.insertGoal(
                    GoalHistoryEntity(
                        goalMinutes = clampedMinutes,
                        startDateUtc = dayStartUtc,
                        endDateUtc = null,
                        createdAtUtc = nowUtc
                    )
                )
            }

            preferencesRepository.setDailyScreenTimeGoalMinutes(clampedMinutes)
            recalculateGamification()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recalculateGamification(): Result<GamificationSnapshot> = withContext(Dispatchers.IO) {
        try {
            ensureGoalHistoryExists()

            val nowUtc = System.currentTimeMillis()
            val todayStartUtc = roundToDayStart(nowUtc)
            val goals = goalHistoryDao.getAllGoalsSync().sortedBy { it.startDateUtc }
            val lastCalculatedDateUtc = readLongMetadata(LAST_CALCULATED_DATE_KEY)
            val baseCurrentStreak = readIntMetadata(BASE_CURRENT_STREAK_KEY)
            val baseLongestStreak = readIntMetadata(BASE_LONGEST_STREAK_KEY)
            val baseTotalPerfectDays = readIntMetadata(BASE_TOTAL_PERFECT_DAYS_KEY)
            val lastSeenAchievementAtUtc = readLongMetadata(LAST_SEEN_ACHIEVEMENT_AT_UTC_KEY) ?: 0L
            val finalizedStartUtc = determineFinalizedStart(lastCalculatedDateUtc, goals, todayStartUtc)
            val usageByDay = loadUsageByDay(finalizedStartUtc, todayStartUtc + MILLIS_PER_DAY)

            var updatedCurrentStreak = baseCurrentStreak
            var updatedLongestStreak = baseLongestStreak
            var updatedTotalPerfectDays = baseTotalPerfectDays
            var processedFinalizedDays = false

            var dayUtc = finalizedStartUtc
            while (dayUtc < todayStartUtc) {
                processedFinalizedDays = true
                val goalForDay = findGoalForDate(goals, dayUtc)
                val goalMinutes = goalForDay?.goalMinutes ?: preferencesRepository.dailyScreenTimeGoalMinutes.first()
                val usageMillis = usageByDay[dayUtc] ?: 0L
                val isPerfect = usageMillis <= goalMinutes.toLong() * 60_000L

                if (isPerfect) {
                    if (perfectDayDao.getPerfectDay(dayUtc) == null) {
                        perfectDayDao.upsertPerfectDay(
                            PerfectDayEntity(
                                dateUtc = dayUtc,
                                totalScreenTimeMillis = usageMillis,
                                goalMinutes = goalMinutes,
                                calculatedAtUtc = nowUtc
                            )
                        )
                        updatedTotalPerfectDays += 1
                    }

                    updatedCurrentStreak += 1
                    if (updatedCurrentStreak > updatedLongestStreak) {
                        updatedLongestStreak = updatedCurrentStreak
                    }

                    insertAchievementsIfNeeded(
                        dayUtc = dayUtc,
                        currentStreak = updatedCurrentStreak,
                        totalPerfectDays = updatedTotalPerfectDays,
                        isFinalized = true,
                        nowUtc = nowUtc
                    )
                } else {
                    if (perfectDayDao.getPerfectDay(dayUtc) != null) {
                        perfectDayDao.deletePerfectDay(dayUtc)
                    }
                    updatedCurrentStreak = 0
                }

                dayUtc += MILLIS_PER_DAY
            }

            if (processedFinalizedDays) {
                saveBaseMetadata(
                    lastCalculatedDateUtc = todayStartUtc - MILLIS_PER_DAY,
                    currentStreak = updatedCurrentStreak,
                    longestStreak = updatedLongestStreak,
                    totalPerfectDays = updatedTotalPerfectDays
                )
            }

            val todayGoal = findGoalForDate(goals, todayStartUtc)
                ?: GoalHistoryEntity(
                    goalMinutes = preferencesRepository.dailyScreenTimeGoalMinutes.first(),
                    startDateUtc = todayStartUtc,
                    endDateUtc = null,
                    createdAtUtc = nowUtc
                )
            val todayUsageMillis = usageByDay[todayStartUtc] ?: 0L
            val todayPerfect = todayUsageMillis <= todayGoal.goalMinutes.toLong() * 60_000L
            val existingTodayPerfectDay = perfectDayDao.getPerfectDay(todayStartUtc)

            if (todayPerfect) {
                if (existingTodayPerfectDay == null) {
                    perfectDayDao.upsertPerfectDay(
                        PerfectDayEntity(
                            dateUtc = todayStartUtc,
                            totalScreenTimeMillis = todayUsageMillis,
                            goalMinutes = todayGoal.goalMinutes,
                            calculatedAtUtc = nowUtc
                        )
                    )
                }
            } else if (existingTodayPerfectDay != null) {
                perfectDayDao.deletePerfectDay(todayStartUtc)
            }

            val currentStreakSnapshot = if (todayPerfect) updatedCurrentStreak + 1 else 0
            val totalPerfectDaysSnapshot = updatedTotalPerfectDays + if (todayPerfect) 1 else 0
            val longestStreakSnapshot = maxOf(updatedLongestStreak, currentStreakSnapshot)

            val achievementGroups = loadGroupedAchievements()
            val achievementRecords = achievementGroups.flatMap { it.achievements }
            val totalXp = achievementRecords.sumOf { it.xpEarned }
            val todayXp = achievementRecords
                .filter { roundToDayStart(it.achievedAtUtc) == todayStartUtc }
                .sumOf { it.xpEarned }
            val levelProgress = calculateLevelProgress(totalXp)
            val latestAnnouncement = achievementRecords
                .filter { it.achievedAtUtc > lastSeenAchievementAtUtc }
                .maxWithOrNull(
                    compareBy<com.librefocus.models.AchievementRecord> { it.achievedAtUtc }
                        .thenBy { it.xpEarned }
                )
                ?.let { record ->
                    AchievementAnnouncement(
                        type = record.type,
                        achievedAtUtc = record.achievedAtUtc,
                        xpEarned = record.xpEarned,
                        level = levelProgress.level,
                        totalXp = totalXp,
                        xpIntoCurrentLevel = levelProgress.xpIntoCurrentLevel,
                        xpForNextLevel = levelProgress.xpForNextLevel,
                        xpToNextLevel = levelProgress.xpToNextLevel
                    )
                }

            Result.success(
                GamificationSnapshot(
                    currentGoalMinutes = todayGoal.goalMinutes,
                    currentStreak = currentStreakSnapshot,
                    longestStreak = longestStreakSnapshot,
                    totalPerfectDays = totalPerfectDaysSnapshot,
                    totalXp = totalXp,
                    todayXp = todayXp,
                    levelProgress = levelProgress,
                    achievementGroups = achievementGroups,
                    latestAchievementAnnouncement = latestAnnouncement
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAchievementAnnouncementSeen(achievedAtUtc: Long) = withContext(Dispatchers.IO) {
        upsertMetadata(LAST_SEEN_ACHIEVEMENT_AT_UTC_KEY, achievedAtUtc)
    }

    private suspend fun ensureGoalHistoryExists() {
        if (goalHistoryDao.getActiveGoal() == null && goalHistoryDao.getEarliestGoalStartDate() == null) {
            val nowUtc = System.currentTimeMillis()
            goalHistoryDao.insertGoal(
                GoalHistoryEntity(
                    goalMinutes = preferencesRepository.dailyScreenTimeGoalMinutes.first(),
                    startDateUtc = roundToDayStart(nowUtc),
                    endDateUtc = null,
                    createdAtUtc = nowUtc
                )
            )
        }
    }

    private suspend fun loadUsageByDay(startUtc: Long, endUtcExclusive: Long): Map<Long, Long> {
        if (startUtc >= endUtcExclusive) return emptyMap()
        val totals = mutableMapOf<Long, Long>()
        hourlyAppUsageDao.getUsageInTimeRangeOnce(startUtc, endUtcExclusive).forEach { entry ->
            val dayStartUtc = roundToDayStart(entry.hourStartUtc)
            totals[dayStartUtc] = (totals[dayStartUtc] ?: 0L) + entry.usageDurationMillis
        }
        return totals
    }

    private fun determineFinalizedStart(
        lastCalculatedDateUtc: Long?,
        goals: List<GoalHistoryEntity>,
        todayStartUtc: Long
    ): Long {
        val earliestGoalStart = goals.minOfOrNull { it.startDateUtc } ?: todayStartUtc
        return when (val lastDate = lastCalculatedDateUtc) {
            null -> earliestGoalStart
            else -> maxOf(lastDate + MILLIS_PER_DAY, earliestGoalStart)
        }
    }

    private fun findGoalForDate(goals: List<GoalHistoryEntity>, dateUtc: Long): GoalHistoryEntity? {
        return goals.lastOrNull { goal ->
            goal.startDateUtc <= dateUtc && (goal.endDateUtc == null || goal.endDateUtc > dateUtc)
        }
    }

    private suspend fun insertAchievementsIfNeeded(
        dayUtc: Long,
        currentStreak: Int,
        totalPerfectDays: Int,
        isFinalized: Boolean,
        nowUtc: Long
    ) {
        if (!isFinalized) return

        insertCurrentStreakMilestoneIfNeeded(
            dayUtc = dayUtc,
            currentStreak = currentStreak,
            nowUtc = nowUtc
        )

        if (currentStreak > 0 && currentStreak % 7 == 0) {
            upsertAchievement(
                type = AchievementType.PERFECT_WEEK,
                sourceDateUtc = dayUtc,
                achievedAtUtc = nowUtc,
                occurrenceCount = currentStreak / 7,
                thresholdValue = 7
            )
        }

        if (isFriday(dayUtc) && isPerfectWeekdayBlock(dayUtc)) {
            upsertAchievement(
                type = AchievementType.PERFECT_WEEKDAYS,
                sourceDateUtc = dayUtc,
                achievedAtUtc = nowUtc,
                occurrenceCount = achievementDao.countAchievementsByType(AchievementType.PERFECT_WEEKDAYS.name) + 1,
                thresholdValue = 5
            )
        }

        if (isSunday(dayUtc) && isPerfectWeekend(dayUtc)) {
            upsertAchievement(
                type = AchievementType.PERFECT_WEEKEND,
                sourceDateUtc = dayUtc,
                achievedAtUtc = nowUtc,
                occurrenceCount = achievementDao.countAchievementsByType(AchievementType.PERFECT_WEEKEND.name) + 1,
                thresholdValue = 2
            )

            if (countConsecutivePerfectWeekends(dayUtc) == 3) {
                insertSingleOccurrenceAchievement(
                    type = AchievementType.WEEKEND_WARRIOR,
                    sourceDateUtc = dayUtc,
                    achievedAtUtc = nowUtc,
                    occurrenceCount = 3,
                    thresholdValue = 3
                )
            }
        }

        if (isMonthEnd(dayUtc)) {
            val monthStartUtc = startOfMonth(dayUtc)
            val nextMonthStartUtc = nextMonthStart(dayUtc)
            val daysInMonth = daysInMonth(dayUtc)
            val perfectDaysInMonth = perfectDayDao.getPerfectDaysInRange(monthStartUtc, nextMonthStartUtc).size
            if (perfectDaysInMonth == daysInMonth) {
                upsertAchievement(
                    type = AchievementType.PERFECT_MONTH,
                    sourceDateUtc = dayUtc,
                    achievedAtUtc = nowUtc,
                    occurrenceCount = achievementDao.countAchievementsByType(AchievementType.PERFECT_MONTH.name) + 1,
                    thresholdValue = daysInMonth
                )
            }
        }

        val milestoneType = AchievementType.milestoneForPerfectDays(totalPerfectDays)
        if (milestoneType != null) {
            upsertAchievement(
                type = milestoneType,
                sourceDateUtc = dayUtc,
                achievedAtUtc = nowUtc,
                occurrenceCount = 1,
                thresholdValue = milestoneType.thresholdValue
            )
        }
    }

    private suspend fun insertCurrentStreakMilestoneIfNeeded(
        dayUtc: Long,
        currentStreak: Int,
        nowUtc: Long
    ) {
        val milestoneType = AchievementType.currentStreakMilestone(currentStreak) ?: return
        insertSingleOccurrenceAchievement(
            type = milestoneType,
            sourceDateUtc = dayUtc,
            achievedAtUtc = nowUtc,
            occurrenceCount = currentStreak,
            thresholdValue = milestoneType.thresholdValue
        )
    }

    private suspend fun insertSingleOccurrenceAchievement(
        type: AchievementType,
        sourceDateUtc: Long,
        achievedAtUtc: Long,
        occurrenceCount: Int,
        thresholdValue: Int?
    ) {
        if (achievementDao.countAchievementsByType(type.name) > 0) return
        upsertAchievement(
            type = type,
            sourceDateUtc = sourceDateUtc,
            achievedAtUtc = achievedAtUtc,
            occurrenceCount = occurrenceCount,
            thresholdValue = thresholdValue
        )
    }

    private suspend fun upsertAchievement(
        type: AchievementType,
        sourceDateUtc: Long,
        achievedAtUtc: Long,
        occurrenceCount: Int,
        thresholdValue: Int?
    ) {
        if (achievementDao.getAchievementByTypeAndSourceDate(type.name, sourceDateUtc) != null) return
        achievementDao.insertAchievement(
            AchievementEntity(
                type = type.name,
                achievedAtUtc = achievedAtUtc,
                sourceDateUtc = sourceDateUtc,
                occurrenceCount = occurrenceCount,
                thresholdValue = thresholdValue
            )
        )
    }

    private suspend fun isPerfectWeekdayBlock(dayUtc: Long): Boolean {
        val weekStartUtc = startOfWeek(dayUtc)
        val nextWeekStartUtc = weekStartUtc + 5L * MILLIS_PER_DAY
        return perfectDayDao.getPerfectDaysInRange(weekStartUtc, nextWeekStartUtc).size == 5
    }

    private suspend fun isPerfectWeekend(dayUtc: Long): Boolean {
        val saturdayStartUtc = dayUtc - MILLIS_PER_DAY
        val sundayEndUtc = dayUtc + MILLIS_PER_DAY
        return perfectDayDao.getPerfectDaysInRange(saturdayStartUtc, sundayEndUtc).size == 2
    }

    private suspend fun countConsecutivePerfectWeekends(sundayUtc: Long): Int {
        var weekendEndUtc = sundayUtc
        var streak = 0

        while (isPerfectWeekend(weekendEndUtc)) {
            streak += 1
            weekendEndUtc -= 7L * MILLIS_PER_DAY
        }

        return streak
    }

    private fun isFriday(dayUtc: Long): Boolean {
        return utcDate(dayUtc).dayOfWeek == DayOfWeek.FRIDAY
    }

    private fun isSunday(dayUtc: Long): Boolean {
        return utcDate(dayUtc).dayOfWeek == DayOfWeek.SUNDAY
    }

    private fun startOfWeek(dayUtc: Long): Long {
        val monday = utcDate(dayUtc).with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return monday.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    }

    private fun utcDate(dayUtc: Long): LocalDate {
        return Instant.ofEpochMilli(dayUtc).atZone(ZoneOffset.UTC).toLocalDate()
    }

    private suspend fun loadGroupedAchievements(): List<AchievementGroup> {
        val records = achievementDao.getAllAchievementsSync()
            .mapNotNull { it.toRecord() }
            .sortedByDescending { it.achievedAtUtc }

        return records
            .groupBy { it.type }
            .map { (type, groupedRecords) ->
                AchievementGroup(type = type, achievements = groupedRecords)
            }
            .sortedWith(compareBy({ it.type.category.ordinal }, { it.type.displayName }))
    }

    private suspend fun saveBaseMetadata(
        lastCalculatedDateUtc: Long,
        currentStreak: Int,
        longestStreak: Int,
        totalPerfectDays: Int
    ) {
        upsertMetadata(LAST_CALCULATED_DATE_KEY, lastCalculatedDateUtc)
        upsertMetadata(BASE_CURRENT_STREAK_KEY, currentStreak.toLong())
        upsertMetadata(BASE_LONGEST_STREAK_KEY, longestStreak.toLong())
        upsertMetadata(BASE_TOTAL_PERFECT_DAYS_KEY, totalPerfectDays.toLong())
    }

    private suspend fun readLongMetadata(key: String): Long? {
        return syncMetadataDao.getMetadata(key)?.valueUtc
    }

    private suspend fun readIntMetadata(key: String): Int {
        return readLongMetadata(key)?.toInt() ?: 0
    }

    private suspend fun upsertMetadata(key: String, valueUtc: Long) {
        syncMetadataDao.insertMetadata(
            SyncMetadataEntity(
                key = key,
                valueUtc = valueUtc
            )
        )
    }

    private fun isMonthEnd(dayUtc: Long): Boolean {
        val date = Instant.ofEpochMilli(dayUtc).atZone(ZoneOffset.UTC).toLocalDate()
        return date.dayOfMonth == YearMonth.from(date).lengthOfMonth()
    }

    private fun startOfMonth(dayUtc: Long): Long {
        val date = Instant.ofEpochMilli(dayUtc).atZone(ZoneOffset.UTC).toLocalDate()
        return date.withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    }

    private fun nextMonthStart(dayUtc: Long): Long {
        val date = Instant.ofEpochMilli(dayUtc).atZone(ZoneOffset.UTC).toLocalDate()
        return date.withDayOfMonth(1).plusMonths(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    }

    private fun daysInMonth(dayUtc: Long): Int {
        val date = Instant.ofEpochMilli(dayUtc).atZone(ZoneOffset.UTC).toLocalDate()
        return YearMonth.from(date).lengthOfMonth()
    }
}