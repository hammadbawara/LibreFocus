package com.librefocus.ui.gamification

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AssistantPhoto
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.librefocus.models.AchievementType

fun AchievementType.detailDescription(): String = when (this) {
    AchievementType.PERFECT_WEEKEND -> "Stay under your goal on both Saturday and Sunday."
    AchievementType.PERFECT_WEEKDAYS -> "Stay under your goal from Monday through Friday."
    AchievementType.PERFECT_WEEK -> "Stay within your goal for seven perfect days in a row."
    AchievementType.PERFECT_MONTH -> "Complete a full month without exceeding your screen time goal."
    AchievementType.FIRST_STEP -> "Stay under your goal for 1 day for the first time."
    AchievementType.DAILY_DISCIPLINE -> "Build a 3-day perfect streak."
    AchievementType.UNBREAKABLE -> "Keep a 14-day perfect streak alive."
    AchievementType.ZEN_MASTER -> "Reach a 30-day perfect streak."
    AchievementType.PERFECT_10 -> "Reach 10 perfect days overall."
    AchievementType.PERFECT_20 -> "Reach 20 perfect days overall."
    AchievementType.PERFECT_30 -> "Reach 30 perfect days overall."
    AchievementType.PERFECT_50 -> "Reach 50 perfect days overall."
    AchievementType.PERFECT_100 -> "Reach 100 perfect days overall."
    AchievementType.PERFECT_365 -> "Reach 365 perfect days overall."
    AchievementType.PERFECT_1000 -> "Reach 1,000 perfect days overall."
    AchievementType.PERFECT_2000 -> "Reach 2,000 perfect days overall."
    AchievementType.PERFECT_3000 -> "Reach 3,000 perfect days overall."
    AchievementType.PERFECT_5000 -> "Reach 5,000 perfect days overall."
    AchievementType.WEEKEND_WARRIOR -> "Earn three perfect weekends in a row."
}

fun AchievementType.awardIcon(): ImageVector = when (this) {
    AchievementType.PERFECT_WEEKEND -> Icons.Filled.CalendarViewWeek
    AchievementType.PERFECT_WEEKDAYS -> Icons.Filled.Work
    AchievementType.PERFECT_WEEK -> Icons.Filled.EmojiEvents
    AchievementType.PERFECT_MONTH -> Icons.Filled.WorkspacePremium
    AchievementType.FIRST_STEP -> Icons.Filled.CheckCircle
    AchievementType.DAILY_DISCIPLINE -> Icons.Filled.ElectricBolt
    AchievementType.UNBREAKABLE -> Icons.Filled.LocalFireDepartment
    AchievementType.ZEN_MASTER -> Icons.Filled.AccessTime
    AchievementType.PERFECT_10 -> Icons.Filled.CheckCircle
    AchievementType.PERFECT_20 -> Icons.Filled.ElectricBolt
    AchievementType.PERFECT_30 -> Icons.Filled.LocalFireDepartment
    AchievementType.PERFECT_50 -> Icons.Filled.Star
    AchievementType.PERFECT_100 -> Icons.Filled.RocketLaunch
    AchievementType.PERFECT_365 -> Icons.Filled.AccessTime
    AchievementType.PERFECT_1000 -> Icons.Filled.AssistantPhoto
    AchievementType.PERFECT_2000 -> Icons.Filled.EmojiEvents
    AchievementType.PERFECT_3000 -> Icons.Filled.WorkspacePremium
    AchievementType.PERFECT_5000 -> Icons.Filled.LocalFireDepartment
    AchievementType.WEEKEND_WARRIOR -> Icons.Filled.EmojiEvents
}

fun AchievementType.awardGradientColors(earned: Boolean): List<Color> = if (!earned) {
    listOf(Color(0xFF9AA0A6), Color(0xFF5F6368))
} else {
    when (this) {
        AchievementType.PERFECT_WEEKEND -> listOf(Color(0xFFFFB74D), Color(0xFFF57C00))
        AchievementType.PERFECT_WEEKDAYS -> listOf(Color(0xFF4FC3F7), Color(0xFF0277BD))
        AchievementType.PERFECT_WEEK -> listOf(Color(0xFFFFD54F), Color(0xFFFF8F00))
        AchievementType.PERFECT_MONTH -> listOf(Color(0xFF81D4FA), Color(0xFF039BE5))
        AchievementType.FIRST_STEP -> listOf(Color(0xFFA5D6A7), Color(0xFF2E7D32))
        AchievementType.DAILY_DISCIPLINE -> listOf(Color(0xFF80CBC4), Color(0xFF00897B))
        AchievementType.UNBREAKABLE -> listOf(Color(0xFFFFAB91), Color(0xFFE64A19))
        AchievementType.ZEN_MASTER -> listOf(Color(0xFFB39DDB), Color(0xFF5E35B1))
        AchievementType.PERFECT_10 -> listOf(Color(0xFFA5D6A7), Color(0xFF2E7D32))
        AchievementType.PERFECT_20 -> listOf(Color(0xFFFFAB91), Color(0xFFE64A19))
        AchievementType.PERFECT_30 -> listOf(Color(0xFFE1BEE7), Color(0xFF8E24AA))
        AchievementType.PERFECT_50 -> listOf(Color(0xFF80CBC4), Color(0xFF00897B))
        AchievementType.PERFECT_100 -> listOf(Color(0xFFFFCC80), Color(0xFFEF6C00))
        AchievementType.PERFECT_365 -> listOf(Color(0xFF90CAF9), Color(0xFF1565C0))
        AchievementType.PERFECT_1000 -> listOf(Color(0xFFF48FB1), Color(0xFFC2185B))
        AchievementType.PERFECT_2000 -> listOf(Color(0xFFCE93D8), Color(0xFF6A1B9A))
        AchievementType.PERFECT_3000 -> listOf(Color(0xFFAED581), Color(0xFF558B2F))
        AchievementType.PERFECT_5000 -> listOf(Color(0xFFFFE082), Color(0xFFF9A825))
        AchievementType.WEEKEND_WARRIOR -> listOf(Color(0xFFFFD54F), Color(0xFFFBC02D))
    }
}

@Composable
fun AchievementMedalIcon(
    achievementType: AchievementType,
    earned: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = achievementType.awardGradientColors(earned)
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(percent = 50),
            color = Color.Transparent,
            tonalElevation = 0.dp,
            shadowElevation = 10.dp,
            border = if (earned) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = if (earned) {
                                listOf(colors[0], colors[1], colors[0].copy(alpha = 0.92f))
                            } else {
                                listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                )
                            }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(colors = colors),
                            shape = RoundedCornerShape(percent = 50)
                        )
                        .background(
                            brush = Brush.linearGradient(
                                colors = if (earned) {
                                    listOf(
                                        Color.White.copy(alpha = 0.42f),
                                        colors[0].copy(alpha = 0.92f),
                                        colors[1].copy(alpha = 0.72f)
                                    )
                                } else {
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surfaceContainerLow
                                    )
                                }
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = achievementType.awardIcon(),
                        contentDescription = achievementType.displayName,
                        tint = if (earned) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}