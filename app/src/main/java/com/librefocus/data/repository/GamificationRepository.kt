package com.librefocus.data.repository

import android.util.Log
import com.librefocus.data.local.database.dao.BadgeDao
import com.librefocus.data.local.database.dao.ChallengeDao
import com.librefocus.data.local.database.dao.GamificationStatsDao
import com.librefocus.data.local.database.dao.HourlyAppUsageDao
import com.librefocus.data.local.database.dao.LeaderboardEntryDao
import com.librefocus.data.local.database.dao.StreakDao
import com.librefocus.data.local.database.entity.BadgeEntity
import com.librefocus.data.local.database.entity.ChallengeEntity
import com.librefocus.data.local.database.entity.GamificationStatsEntity
import com.librefocus.data.local.database.entity.LeaderboardEntryEntity
import com.librefocus.data.local.database.entity.StreakEntity
import com.librefocus.models.Badge
import com.librefocus.models.BadgeType
import com.librefocus.models.Challenge
import com.librefocus.models.ChallengeType
import com.librefocus.models.GamificationStats
import com.librefocus.models.LeaderboardEntry
import com.librefocus.models.Streak
import com.librefocus.utils.roundToDayStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Repository for managing gamification features.
 * Handles points calculation, streak tracking, badge unlocking, and challenges.
 */
class GamificationRepository(
    private val gamificationStatsDao: GamificationStatsDao,
    private val badgeDao: BadgeDao,
    private val challengeDao: ChallengeDao,
    private val leaderboardEntryDao: LeaderboardEntryDao,
    private val streakDao: StreakDao,
    private val hourlyAppUsageDao: HourlyAppUsageDao
) {

    companion object {
        private const val TAG = "GamificationRepository"
        private const val DEFAULT_DAILY_SCREEN_TIME_LIMIT = 180 // minutes (3 hours)
        private const val POINTS_PER_MINUTE_SAVED = 1
        private const val BONUS_POINTS_UNDER_LIMIT = 50
        private const val BONUS_POINTS_DAILY_GOAL = 100
    }

    // ======================== Statistics ========================

    fun getGamificationStats(): Flow<GamificationStats> {
        return gamificationStatsDao.getStats().map { entity ->
            entity?.let {
                GamificationStats(
                    totalPoints = it.totalPoints,
                    currentStreak = it.currentStreak,
                    longestStreak = it.longestStreak,
                    lastActiveDate = it.lastActiveDate,
                    unlockedBadgeCount = 0,
                    completedChallengeCount = 0
                )
            } ?: GamificationStats()
        }
    }

    suspend fun getStats(): GamificationStats? = withContext(Dispatchers.IO) {
        gamificationStatsDao.getStatsOnce()?.let {
            GamificationStats(
                totalPoints = it.totalPoints,
                currentStreak = it.currentStreak,
                longestStreak = it.longestStreak,
                lastActiveDate = it.lastActiveDate
            )
        }
    }

    // ======================== Badges ========================

    fun getBadges(): Flow<List<Badge>> {
        return badgeDao.getAllBadges().map { entities ->
            entities.map { entity ->
                Badge(
                    id = entity.id,
                    title = entity.title,
                    description = entity.description,
                    icon = entity.icon,
                    isUnlocked = entity.isUnlocked,
                    unlockedAt = entity.unlockedAt,
                    type = entity.type
                )
            }
        }
    }

    suspend fun initializeBadges() = withContext(Dispatchers.IO) {
        val existingBadges = badgeDao.getAllBadgesOnce()
        if (existingBadges.isEmpty()) {
            val defaultBadges = listOf(
                BadgeEntity(
                    title = "First Step",
                    description = "Complete your first daily goal",
                    icon = "ic_badge_first_step",
                    type = BadgeType.FIRST_STEP
                ),
                BadgeEntity(
                    title = "Focused Mind",
                    description = "Maintain a 3-day streak",
                    icon = "ic_badge_focused_mind",
                    type = BadgeType.FOCUSED_MIND
                ),
                BadgeEntity(
                    title = "Discipline Master",
                    description = "Maintain a 7-day streak",
                    icon = "ic_badge_discipline_master",
                    type = BadgeType.DISCIPLINE_MASTER
                ),
                BadgeEntity(
                    title = "Week Warrior",
                    description = "Maintain a 14-day streak",
                    icon = "ic_badge_week_warrior",
                    type = BadgeType.WEEK_WARRIOR
                ),
                BadgeEntity(
                    title = "Month Master",
                    description = "Maintain a 30-day streak",
                    icon = "ic_badge_month_master",
                    type = BadgeType.MONTH_MASTER
                ),
                BadgeEntity(
                    title = "Limit Achieved",
                    description = "Complete 10 limit achievements",
                    icon = "ic_badge_limit_achieved",
                    type = BadgeType.LIMIT_ACHIEVED
                ),
                BadgeEntity(
                    title = "Challenge Master",
                    description = "Complete 20 challenges",
                    icon = "ic_badge_challenge_master",
                    type = BadgeType.CHALLENGE_MASTER
                )
            )
            badgeDao.insertBadges(defaultBadges)
            Log.d(TAG, "Initialized ${defaultBadges.size} badges")
        }
    }

    suspend fun checkAndUnlockBadges(stats: GamificationStats) = withContext(Dispatchers.IO) {
        try {
            val existingBadges = badgeDao.getAllBadgesOnce()
            val now = System.currentTimeMillis()

            for (badge in existingBadges) {
                if (badge.isUnlocked) continue // Skip already unlocked

                val shouldUnlock = when (badge.type) {
                    BadgeType.FIRST_STEP -> stats.currentStreak >= 1 && stats.totalPoints > 0
                    BadgeType.FOCUSED_MIND -> stats.currentStreak >= 3
                    BadgeType.DISCIPLINE_MASTER -> stats.currentStreak >= 7
                    BadgeType.WEEK_WARRIOR -> stats.currentStreak >= 14
                    BadgeType.MONTH_MASTER -> stats.currentStreak >= 30
                    BadgeType.LIMIT_ACHIEVED -> stats.totalPoints >= 500
                    BadgeType.CHALLENGE_MASTER -> stats.completedChallengeCount >= 20
                    else -> false
                }

                if (shouldUnlock) {
                    val unlockedBadge = badge.copy(isUnlocked = true, unlockedAt = now)
                    badgeDao.updateBadge(unlockedBadge)
                    Log.d(TAG, "Unlocked badge: ${badge.title}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking badges", e)
        }
    }

    // ======================== Challenges ========================

    fun getChallenges(): Flow<List<Challenge>> {
        return challengeDao.getAllChallenges().map { entities ->
            entities.map { entity ->
                Challenge(
                    id = entity.id,
                    title = entity.title,
                    description = entity.description,
                    type = ChallengeType.valueOf(entity.type),
                    targetValue = entity.targetValue,
                    currentProgress = entity.currentProgress,
                    isCompleted = entity.isCompleted,
                    completedAt = entity.completedAt,
                    date = entity.date,
                    rewardPoints = entity.rewardPoints
                )
            }
        }
    }

    suspend fun generateTodayChallenge() = withContext(Dispatchers.IO) {
        try {
            val today = roundToDayStart(System.currentTimeMillis())
            val existingChallenges = challengeDao.getChallengesForDateOnce(today)

            if (existingChallenges.isEmpty()) {
                val challenge = ChallengeEntity(
                    title = "Keep it Under Control",
                    description = "Use your phone for less than 3 hours today",
                    type = ChallengeType.LIMIT_USAGE.name,
                    targetValue = 180, // 3 hours in minutes
                    date = today,
                    rewardPoints = 100
                )
                challengeDao.insertChallenge(challenge)
                Log.d(TAG, "Generated today's challenge")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating challenge", e)
        }
    }

    suspend fun updateChallengeProgress(screenTimeTodayMinutes: Int) = withContext(Dispatchers.IO) {
        try {
            val today = roundToDayStart(System.currentTimeMillis())
            val challenges = challengeDao.getChallengesForDateOnce(today)

            for (challenge in challenges) {
                if (!challenge.isCompleted) {
                    val updatedChallenge = challenge.copy(
                        currentProgress = screenTimeTodayMinutes
                    )
                    challengeDao.updateChallenge(updatedChallenge)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating challenge progress", e)
        }
    }

    suspend fun completeChallenge(challengeId: Int) = withContext(Dispatchers.IO) {
        try {
            val challenge = challengeDao.getChallengeById(challengeId) ?: return@withContext
            if (!challenge.isCompleted) {
                val completed = challenge.copy(isCompleted = true, completedAt = System.currentTimeMillis())
                challengeDao.updateChallenge(completed)
                addPoints(challenge.rewardPoints)
                Log.d(TAG, "Challenge completed with ${challenge.rewardPoints} points")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error completing challenge", e)
        }
    }

    // ======================== Streaks ========================

    fun getStreaks(): Flow<List<Streak>> {
        return streakDao.getAllStreaks().map { entities ->
            entities.map { entity ->
                Streak(
                    id = entity.id,
                    date = entity.date,
                    goalMet = entity.goalMet,
                    screenTimeLimitMinutes = entity.screenTimeLimitMinutes,
                    actualScreenTimeMinutes = entity.actualScreenTimeMinutes
                )
            }
        }
    }

    suspend fun updateTodayStreak(screenTimeMinutes: Int) = withContext(Dispatchers.IO) {
        try {
            val today = roundToDayStart(System.currentTimeMillis())
            val limit = DEFAULT_DAILY_SCREEN_TIME_LIMIT

            val goalMet = screenTimeMinutes <= limit
            val streak = StreakEntity(
                date = today,
                goalMet = goalMet,
                screenTimeLimitMinutes = limit,
                actualScreenTimeMinutes = screenTimeMinutes
            )
            streakDao.insertStreak(streak)

            if (goalMet) {
                updateCurrentStreak()
                completeAndGenerateChallenge(screenTimeMinutes, limit)
            } else {
                resetCurrentStreak()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating streak", e)
        }
    }

    private suspend fun updateCurrentStreak() {
        try {
            val stats = gamificationStatsDao.getStatsOnce()
            val today = roundToDayStart(System.currentTimeMillis())

            val newStreak = if (stats != null && stats.lastActiveDate > 0) {
                val daysSinceLastActive = (today - stats.lastActiveDate) / (24 * 60 * 60 * 1000)
                if (daysSinceLastActive == 1L) {
                    stats.currentStreak + 1
                } else if (daysSinceLastActive == 0L) {
                    stats.currentStreak
                } else {
                    1
                }
            } else {
                1
            }

            val updatedStats = GamificationStatsEntity(
                totalPoints = stats?.totalPoints ?: 0,
                currentStreak = newStreak,
                longestStreak = maxOf(newStreak, stats?.longestStreak ?: 0),
                lastActiveDate = today,
                lastStreakResetDate = stats?.lastStreakResetDate ?: 0L
            )
            gamificationStatsDao.insertOrUpdate(updatedStats)
            Log.d(TAG, "Updated streak to $newStreak")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating current streak", e)
        }
    }

    private suspend fun resetCurrentStreak() {
        try {
            val stats = gamificationStatsDao.getStatsOnce()
            val updatedStats = GamificationStatsEntity(
                totalPoints = stats?.totalPoints ?: 0,
                currentStreak = 0,
                longestStreak = stats?.longestStreak ?: 0,
                lastActiveDate = stats?.lastActiveDate ?: 0L,
                lastStreakResetDate = System.currentTimeMillis()
            )
            gamificationStatsDao.insertOrUpdate(updatedStats)
            Log.d(TAG, "Reset current streak")
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting streak", e)
        }
    }

    private suspend fun completeAndGenerateChallenge(screenTime: Int, limit: Int) {
        try {
            val today = roundToDayStart(System.currentTimeMillis())
            val challenges = challengeDao.getChallengesForDateOnce(today)

            for (challenge in challenges) {
                if (!challenge.isCompleted && screenTime <= limit) {
                    completeChallenge(challenge.id)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error completing challenge", e)
        }
    }

    // ======================== Points ========================

    suspend fun addPoints(points: Int) = withContext(Dispatchers.IO) {
        try {
            val stats = gamificationStatsDao.getStatsOnce()
            val newTotal = (stats?.totalPoints ?: 0) + points
            val updatedStats = GamificationStatsEntity(
                totalPoints = newTotal,
                currentStreak = stats?.currentStreak ?: 0,
                longestStreak = stats?.longestStreak ?: 0,
                lastActiveDate = stats?.lastActiveDate ?: 0L,
                lastStreakResetDate = stats?.lastStreakResetDate ?: 0L
            )
            gamificationStatsDao.insertOrUpdate(updatedStats)
            Log.d(TAG, "Added $points points. Total: $newTotal")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding points", e)
        }
    }

    // ======================== Leaderboard ========================

    fun getLeaderboard(): Flow<List<LeaderboardEntry>> {
        return leaderboardEntryDao.getLeaderboard().map { entities ->
            entities.mapIndexed { index, entity ->
                LeaderboardEntry(
                    id = entity.id,
                    userId = entity.userId,
                    username = entity.username,
                    points = entity.points,
                    rank = index + 1,
                    updatedAt = entity.updatedAt
                )
            }
        }
    }

    suspend fun initializeLocalLeaderboard() = withContext(Dispatchers.IO) {
        try {
            val deviceId = UUID.randomUUID().toString()
            val entry = LeaderboardEntryEntity(
                userId = deviceId,
                username = "You",
                points = 0,
                updatedAt = System.currentTimeMillis()
            )
            leaderboardEntryDao.insertEntry(entry)
            Log.d(TAG, "Initialized local leaderboard entry")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing leaderboard", e)
        }
    }

    suspend fun updateLeaderboardPoints() = withContext(Dispatchers.IO) {
        try {
            val stats = gamificationStatsDao.getStatsOnce() ?: return@withContext
            val entry = leaderboardEntryDao.getTopEntries(1).firstOrNull() ?: return@withContext

            val updatedEntry = entry.copy(
                points = stats.totalPoints,
                updatedAt = System.currentTimeMillis()
            )
            leaderboardEntryDao.updateEntry(updatedEntry)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating leaderboard", e)
        }
    }

    // ======================== Daily Sync ========================

    suspend fun syncDailyGamification() = withContext(Dispatchers.IO) {
        try {
            val today = roundToDayStart(System.currentTimeMillis())
            val todayUsage = hourlyAppUsageDao.getUsageInTimeRangeOnce(
                today,
                today + 24 * 60 * 60 * 1000
            )

            val screenTimeMinutes = todayUsage.sumOf { it.usageDurationMillis }.toLong() / (60 * 1000)
            updateTodayStreak(screenTimeMinutes.toInt())
            updateChallengeProgress(screenTimeMinutes.toInt())

            val stats = getStats()
            if (stats != null) {
                checkAndUnlockBadges(stats)
                updateLeaderboardPoints()
            }

            Log.d(TAG, "Daily gamification sync completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during daily sync", e)
        }
    }

    suspend fun resetAllGamification() = withContext(Dispatchers.IO) {
        try {
            gamificationStatsDao.clear()
            badgeDao.clear()
            challengeDao.clear()
            leaderboardEntryDao.clear()
            streakDao.clear()
            Log.d(TAG, "All gamification data cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting gamification", e)
        }
    }
}
