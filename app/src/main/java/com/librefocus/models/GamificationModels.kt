package com.librefocus.models

import com.librefocus.data.local.database.entity.AchievementEntity

enum class AchievementCategory {
    REPEATABLE,
    MILESTONE
}

enum class AchievementType(
    val displayName: String,
    val category: AchievementCategory,
    val thresholdValue: Int? = null,
    val xpReward: Int
) {
    PERFECT_WEEKEND("Perfect Weekend", AchievementCategory.REPEATABLE, 2, 8),
    PERFECT_WEEKDAYS("Perfect Weekdays", AchievementCategory.REPEATABLE, 5, 14),
    PERFECT_WEEK("Perfect Week", AchievementCategory.REPEATABLE, 7, 20),
    PERFECT_MONTH("Perfect Month", AchievementCategory.REPEATABLE, null, 40),
    FIRST_STEP("First Step", AchievementCategory.MILESTONE, 1, 5),
    DAILY_DISCIPLINE("Daily Discipline", AchievementCategory.MILESTONE, 3, 12),
    UNBREAKABLE("Unbreakable", AchievementCategory.MILESTONE, 14, 28),
    ZEN_MASTER("Zen Master", AchievementCategory.MILESTONE, 30, 45),
    PERFECT_10("Perfect 10", AchievementCategory.MILESTONE, 10, 15),
    PERFECT_20("Perfect 20", AchievementCategory.MILESTONE, 20, 25),
    PERFECT_30("Perfect 30", AchievementCategory.MILESTONE, 30, 35),
    PERFECT_50("Perfect 50", AchievementCategory.MILESTONE, 50, 50),
    PERFECT_100("Perfect 100", AchievementCategory.MILESTONE, 100, 70),
    PERFECT_365("Perfect 365", AchievementCategory.MILESTONE, 365, 110),
    PERFECT_1000("Perfect 1000", AchievementCategory.MILESTONE, 1000, 160),
    PERFECT_2000("Perfect 2000", AchievementCategory.MILESTONE, 2000, 230),
    PERFECT_3000("Perfect 3000", AchievementCategory.MILESTONE, 3000, 320),
    PERFECT_5000("Perfect 5000", AchievementCategory.MILESTONE, 5000, 450),
    WEEKEND_WARRIOR("Weekend Warrior", AchievementCategory.MILESTONE, 3, 30);

    companion object {
        fun fromStorageValue(value: String): AchievementType? = entries.find { it.name == value }

        fun currentStreakMilestone(currentStreak: Int): AchievementType? {
            return when (currentStreak) {
                1 -> FIRST_STEP
                3 -> DAILY_DISCIPLINE
                14 -> UNBREAKABLE
                30 -> ZEN_MASTER
                else -> null
            }
        }

        fun milestoneForPerfectDays(totalPerfectDays: Int): AchievementType? {
            return when (totalPerfectDays) {
                10 -> PERFECT_10
                20 -> PERFECT_20
                30 -> PERFECT_30
                50 -> PERFECT_50
                100 -> PERFECT_100
                365 -> PERFECT_365
                1000 -> PERFECT_1000
                2000 -> PERFECT_2000
                3000 -> PERFECT_3000
                5000 -> PERFECT_5000
                else -> null
            }
        }
    }
}

data class LevelProgress(
    val level: Int = 1,
    val totalXp: Int = 0,
    val xpIntoCurrentLevel: Int = 0,
    val xpForNextLevel: Int = xpRequiredForNextLevel(1),
    val xpToNextLevel: Int = xpForNextLevel
)

data class AchievementAnnouncement(
    val type: AchievementType,
    val achievedAtUtc: Long,
    val xpEarned: Int,
    val level: Int,
    val totalXp: Int,
    val xpIntoCurrentLevel: Int,
    val xpForNextLevel: Int,
    val xpToNextLevel: Int
)

data class AchievementRecord(
    val type: AchievementType,
    val achievedAtUtc: Long,
    val sourceDateUtc: Long,
    val occurrenceCount: Int,
    val thresholdValue: Int? = null
)

data class AchievementGroup(
    val type: AchievementType,
    val achievements: List<AchievementRecord>
)

data class GamificationSnapshot(
    val currentGoalMinutes: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val totalPerfectDays: Int,
    val totalXp: Int,
    val todayXp: Int,
    val levelProgress: LevelProgress,
    val achievementGroups: List<AchievementGroup>,
    val latestAchievementAnnouncement: AchievementAnnouncement? = null
)

fun AchievementEntity.toRecord(): AchievementRecord? {
    val typeValue = AchievementType.fromStorageValue(type) ?: return null
    return AchievementRecord(
        type = typeValue,
        achievedAtUtc = achievedAtUtc,
        sourceDateUtc = sourceDateUtc,
        occurrenceCount = occurrenceCount,
        thresholdValue = thresholdValue
    )
}

val AchievementRecord.xpEarned: Int
    get() = type.xpReward

val AchievementGroup.totalXp: Int
    get() = achievements.sumOf { it.xpEarned }

fun xpRequiredForNextLevel(level: Int): Int {
    val safeLevel = level.coerceAtLeast(1)
    return 100 + (safeLevel - 1) * 75
}

fun calculateLevelProgress(totalXp: Int): LevelProgress {
    val clampedXp = totalXp.coerceAtLeast(0)
    var remainingXp = clampedXp
    var level = 1
    var xpForNextLevel = xpRequiredForNextLevel(level)

    while (remainingXp >= xpForNextLevel) {
        remainingXp -= xpForNextLevel
        level += 1
        xpForNextLevel = xpRequiredForNextLevel(level)
    }

    return LevelProgress(
        level = level,
        totalXp = clampedXp,
        xpIntoCurrentLevel = clampedXp - remainingXp,
        xpForNextLevel = xpForNextLevel,
        xpToNextLevel = xpForNextLevel - remainingXp
    )
}