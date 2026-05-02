package com.librefocus.models

import com.librefocus.data.local.database.entity.AchievementEntity

enum class AchievementCategory {
    REPEATABLE,
    MILESTONE
}

enum class AchievementType(
    val displayName: String,
    val category: AchievementCategory,
    val thresholdValue: Int? = null
) {
    PERFECT_WEEKEND("Perfect Weekend", AchievementCategory.REPEATABLE, 2),
    PERFECT_WEEKDAYS("Perfect Weekdays", AchievementCategory.REPEATABLE, 5),
    PERFECT_WEEK("Perfect Week", AchievementCategory.REPEATABLE, 7),
    PERFECT_MONTH("Perfect Month", AchievementCategory.REPEATABLE, null),
    FIRST_STEP("First Step", AchievementCategory.MILESTONE, 1),
    DAILY_DISCIPLINE("Daily Discipline", AchievementCategory.MILESTONE, 3),
    UNBREAKABLE("Unbreakable", AchievementCategory.MILESTONE, 14),
    ZEN_MASTER("Zen Master", AchievementCategory.MILESTONE, 30),
    PERFECT_10("Perfect 10", AchievementCategory.MILESTONE, 10),
    PERFECT_20("Perfect 20", AchievementCategory.MILESTONE, 20),
    PERFECT_30("Perfect 30", AchievementCategory.MILESTONE, 30),
    PERFECT_50("Perfect 50", AchievementCategory.MILESTONE, 50),
    PERFECT_100("Perfect 100", AchievementCategory.MILESTONE, 100),
    PERFECT_365("Perfect 365", AchievementCategory.MILESTONE, 365),
    PERFECT_1000("Perfect 1000", AchievementCategory.MILESTONE, 1000),
    PERFECT_2000("Perfect 2000", AchievementCategory.MILESTONE, 2000),
    PERFECT_3000("Perfect 3000", AchievementCategory.MILESTONE, 3000),
    PERFECT_5000("Perfect 5000", AchievementCategory.MILESTONE, 5000),
    WEEKEND_WARRIOR("Weekend Warrior", AchievementCategory.MILESTONE, 3);

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
    val achievementGroups: List<AchievementGroup>
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