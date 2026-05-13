package com.librefocus.ui.gamification

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssistantPhoto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.librefocus.models.AchievementGroup
import com.librefocus.models.AchievementType
import com.librefocus.models.totalXp
import com.librefocus.models.xpEarned
import com.librefocus.ui.common.AppScaffold
import com.librefocus.ui.common.AppTopAppBar
import com.librefocus.ui.navigation.AchievementDetailRoute
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    navController: NavController,
    currentRoute: String?,
    viewModel: GamificationViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val achievementTypes = remember(uiState.achievementGroups) {
        val achievementGroupsByType = uiState.achievementGroups.associateBy { it.type }

        AchievementType.entries.sortedWith(
            compareByDescending<AchievementType> { type ->
                achievementGroupsByType[type]
                    ?.achievements
                    ?.maxOfOrNull { it.achievedAtUtc }
                    ?: Long.MIN_VALUE
            }.thenBy { it.ordinal }
        )
    }

    var showGoalDialog by rememberSaveable { mutableStateOf(false) }
    var goalInput by rememberSaveable { mutableStateOf("") }
    var showGoalMenu by rememberSaveable { mutableStateOf(false) }
    var hasPromptedForMissingGoal by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoading, uiState.currentGoalMinutes) {
        if (uiState.isLoading) return@LaunchedEffect

        if (uiState.currentGoalMinutes > 0) {
            hasPromptedForMissingGoal = false
        } else if (!hasPromptedForMissingGoal) {
            goalInput = ""
            showGoalDialog = true
            hasPromptedForMissingGoal = true
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    AppScaffold(
        topBar = { scrollBehavior ->
            AppTopAppBar(
                title = { Text(text = "Achievements") },
                showNavigationIcon = true,
                onClickNavigationIcon = { navController.navigateUp() },
                actions = {
                    IconButton(onClick = { showGoalMenu = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Change screen time goal"
                        )
                    }
                    DropdownMenu(
                        expanded = showGoalMenu,
                        onDismissRequest = { showGoalMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (uiState.currentGoalMinutes > 0) {
                                        "Change goal"
                                    } else {
                                        "Set goal"
                                    }
                                )
                            },
                            onClick = {
                                goalInput = uiState.currentGoalMinutes.takeIf { it > 0 }?.toString().orEmpty()
                                showGoalMenu = false
                                showGoalDialog = true
                            }
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        LazyVerticalGrid(
            state = rememberLazyGridState(),
            columns = GridCells.Adaptive(minSize = 168.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
//            item(span = { GridItemSpan(maxLineSpan) }) {
//                AchievementHeroCard(
//                    title = "Achievement journey",
//                    subtitle = if (uiState.currentGoalMinutes > 0) {
//                        "Daily goal: ${uiState.currentGoalMinutes} min"
//                    } else {
//                        "Set a daily screen time goal to unlock achievements."
//                    }
//                )
//            }

            if (uiState.currentGoalMinutes <= 0) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    GoalPromptCard(
                        onSetGoal = {
                            goalInput = ""
                            showGoalDialog = true
                        }
                    )
                }
            } else {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LevelProgressCard(
                        level = uiState.level,
                        totalXp = uiState.totalXp,
                        todayXp = uiState.todayXp,
                        xpIntoCurrentLevel = uiState.xpIntoCurrentLevel,
                        xpForNextLevel = uiState.xpForNextLevel,
                        xpToNextLevel = uiState.xpToNextLevel
                    )
                }

                item (span = { GridItemSpan(maxLineSpan) }){
                    SummarySection(
                        currentStreak = uiState.currentStreak,
                        longestStreak = uiState.longestStreak,
                        totalPerfectDays = uiState.totalPerfectDays,
                    )
                }


                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader(title = "Achievements")
                }

                if (AchievementType.entries.all { type -> uiState.achievementGroups.none { it.type == type } }) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        MotivationalEmptyState()
                    }
                }

                items(
                    items = achievementTypes,
                    key = { it.name }
                ) { achievementType ->
                    val group = uiState.achievementGroups.firstOrNull { it.type == achievementType }
                    AchievementCard(
                        achievementType = achievementType,
                        group = group,
                        onClick = {
                            navController.navigate(AchievementDetailRoute.createRoute(achievementType.name))
                        }
                    )
                }
            }
        }
    }

    if (showGoalDialog) {
        GoalDialog(
            currentGoalMinutes = uiState.currentGoalMinutes,
            goalInput = goalInput,
            onGoalInputChange = { goalInput = it.filter(Char::isDigit) },
            onDismiss = { showGoalDialog = false },
            onConfirm = { minutes ->
                viewModel.setDailyGoal(minutes)
                showGoalDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementDetailScreen(
    achievementType: String,
    onBackClick: () -> Unit,
    viewModel: GamificationViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val formattedPreferences by viewModel.formattedPreferences.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val selectedType = remember(achievementType) {
        AchievementType.fromStorageValue(achievementType)
    }
    val selectedGroup = remember(uiState.achievementGroups, selectedType) {
        selectedType?.let { type -> uiState.achievementGroups.firstOrNull { it.type == type } }
    }
    val selectedGroupXp = selectedGroup?.totalXp ?: 0
    val selectedAwardXp = selectedType?.xpReward ?: 0
    val achievementDates = remember(selectedGroup?.achievements) {
        selectedGroup?.achievements.orEmpty().sortedByDescending { it.achievedAtUtc }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    AppScaffold(
        topBar = { scrollBehavior ->
            AppTopAppBar(
                title = {
                    Text(
                        text = selectedType?.displayName ?: "Achievement",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                showNavigationIcon = true,
                onClickNavigationIcon = onBackClick,
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 168.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                InteractiveAchievementShowcase(
                    title = selectedType?.displayName ?: "Achievement",
                    subtitle = selectedType?.detailDescription().orEmpty(),
                    earned = selectedGroup != null,
                    achievementType = selectedType
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                SummarySection(
                    currentStreak = selectedGroup?.achievements?.size ?: 0,
                    longestStreak = selectedGroup?.achievements?.size ?: 0,
                    totalPerfectDays = selectedGroup?.achievements?.size ?: 0,
                    singleMetricLabel = "Total times achieved",
                    singleMetricValue = selectedGroup?.achievements?.size?.toString().orEmpty()
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                XpSummaryCard(
                    level = uiState.level,
                    totalXp = uiState.totalXp,
                    achievementXp = selectedAwardXp,
                    achievementTotalXp = selectedGroupXp,
                    xpIntoCurrentLevel = uiState.xpIntoCurrentLevel,
                    xpForNextLevel = uiState.xpForNextLevel,
                    xpToNextLevel = uiState.xpToNextLevel
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(title = "Achievement dates")
            }

            if (achievementDates.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    MotivationalEmptyState(
                        title = if (selectedGroup == null) {
                            "Not earned yet"
                        } else {
                            "No recorded dates"
                        },
                        subtitle = if (selectedGroup == null) {
                            "Keep going. This award will light up here once you earn it."
                        } else {
                            "Dates will appear here after this achievement is earned."
                        }
                    )
                }
            } else {
                items(
                    items = achievementDates,
                    key = { it.achievedAtUtc }
                ) { record ->
                    AchievementDateRow(
                        achievedAtLabel = formattedPreferences?.formatDateTime(record.achievedAtUtc)
                            ?: record.achievedAtUtc.toString(),
                        sourceDateLabel = formattedPreferences?.formatDate(record.sourceDateUtc)
                            ?: record.sourceDateUtc.toString(),
                        occurrenceCount = record.occurrenceCount,
                        xpEarned = record.xpEarned
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementHeroCard(
    title: String,
    subtitle: String,
    earned: Boolean = true,
    achievementType: AchievementType? = null
) {
    val shape = RoundedCornerShape(28.dp)
    val accentColors = achievementType?.awardGradientColors(earned)
        ?: listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (earned) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = if (earned) {
                            accentColors + MaterialTheme.colorScheme.surface.copy(alpha = 0.18f)
                        } else {
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                                MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        }
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (achievementType != null) {
                    Text(
                        text = achievementType.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (earned) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (earned) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (earned) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
private fun GoalPromptCard(onSetGoal: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.AssistantPhoto,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Set your daily goal",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "A screen time goal is needed before achievements can be calculated and displayed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onSetGoal) {
                Text(text = "Set goal")
            }
        }
    }
}

@Composable
private fun SummarySection(
    currentStreak: Int,
    longestStreak: Int,
    totalPerfectDays: Int,
    singleMetricLabel: String? = null,
    singleMetricValue: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title = if (singleMetricLabel == null) "Summary" else singleMetricLabel)
        if (singleMetricLabel != null && singleMetricValue != null) {
            MetricCard(label = singleMetricLabel, value = singleMetricValue)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard(
                    label = "Current streak",
                    value = currentStreak.toString(),
                    icon = Icons.Filled.LocalFireDepartment,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "Longest streak",
                    value = longestStreak.toString(),
                    icon = Icons.Filled.EmojiEvents,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "Perfect days",
                    value = totalPerfectDays.toString(),
                    icon = Icons.Filled.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun LevelProgressCard(
    level: Int,
    totalXp: Int,
    todayXp: Int,
    xpIntoCurrentLevel: Int,
    xpForNextLevel: Int,
    xpToNextLevel: Int
) {
    val progress = if (xpForNextLevel > 0) {
        xpIntoCurrentLevel.toFloat() / xpForNextLevel.toFloat()
    } else {
        0f
    }.coerceIn(0f, 1f)
    val todayXpColor = Color(0xFF2E7D32)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Level progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Level $level",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
                ) {
                    Text(
                        text = "$xpToNextLevel XP to next",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = buildAnnotatedString {
                    append("XP ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(totalXp.toString())
                    }
                    append(" + ")
                    withStyle(
                        style = SpanStyle(
                            color = todayXpColor,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("$todayXp today")
                    }
                },
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "$xpIntoCurrentLevel / $xpForNextLevel XP in this level",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun XpSummaryCard(
    level: Int,
    totalXp: Int,
    achievementXp: Int,
    achievementTotalXp: Int,
    xpIntoCurrentLevel: Int,
    xpForNextLevel: Int,
    xpToNextLevel: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "XP and level",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Level $level • $totalXp total XP",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            LinearProgressIndicator(
                progress = {
                    if (xpForNextLevel > 0) {
                        xpIntoCurrentLevel.toFloat() / xpForNextLevel.toFloat()
                    } else {
                        0f
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "$xpToNextLevel XP to the next level",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "This goal gives $achievementXp XP per award and $achievementTotalXp XP total so far.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun MotivationalEmptyState(
    title: String = "No achievements yet",
    subtitle: String = "Keep following your goal. Your first awards will appear here soon."
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.RocketLaunch,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AchievementCard(
    achievementType: AchievementType,
    group: AchievementGroup?,
    onClick: () -> Unit
) {
    val earned = group != null
    val cardShape = RoundedCornerShape(24.dp)
    val accentColors = achievementType.awardGradientColors(earned)
    val countLabel = when {
        group == null -> "Locked"
        achievementType.category == com.librefocus.models.AchievementCategory.REPEATABLE -> {
            "${group.achievements.size} earned"
        }
        else -> "Unlocked"
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (earned) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (earned) 14.dp else 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = if (earned) {
                            accentColors + MaterialTheme.colorScheme.surface.copy(alpha = 0.18f)
                        } else {
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                                MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        }
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AchievementMedalIcon(
                achievementType = achievementType,
                earned = earned,
                modifier = Modifier.size(92.dp)
            )

            Text(
                text = achievementType.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = if (earned) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (achievementType.category == com.librefocus.models.AchievementCategory.REPEATABLE || earned) {
                Text(
                    text = countLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (earned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun InteractiveAchievementShowcase(
    title: String,
    subtitle: String,
    earned: Boolean,
    achievementType: AchievementType?
) {
    val hapticFeedback = LocalHapticFeedback.current
    var rotationXTarget by remember(achievementType?.name ?: title) { mutableFloatStateOf(0f) }
    var rotationYTarget by remember(achievementType?.name ?: title) { mutableFloatStateOf(0f) }
    val rotationX by animateFloatAsState(targetValue = rotationXTarget, label = "showcaseRotationX")
    val rotationY by animateFloatAsState(targetValue = rotationYTarget, label = "showcaseRotationY")
    val scale by animateFloatAsState(targetValue = if (earned) 1f else 0.98f, label = "showcaseScale")
    val showcaseType = achievementType ?: AchievementType.PERFECT_10
    val accentColors = showcaseType.awardGradientColors(earned)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.rotationX = rotationX
                this.rotationY = rotationY
                this.scaleX = scale
                this.scaleY = scale
            }
            .pointerInput(achievementType?.name ?: title) {
                detectDragGestures(
                    onDragEnd = {
                        rotationXTarget = 0f
                        rotationYTarget = 0f
                    },
                    onDragCancel = {
                        rotationXTarget = 0f
                        rotationYTarget = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        rotationYTarget = (rotationYTarget + dragAmount.x * 0.45f).coerceIn(-18f, 18f)
                        rotationXTarget = (rotationXTarget - dragAmount.y * 0.45f).coerceIn(-18f, 18f)
                    }
                )
            }
            .clickable {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            },
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (earned) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = if (earned) {
                            accentColors + MaterialTheme.colorScheme.surface.copy(alpha = 0.12f)
                        } else {
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                                MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        }
                    )
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AchievementMedalIcon(
                achievementType = showcaseType,
                earned = earned,
                modifier = Modifier.size(180.dp)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = if (earned) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (earned) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MedalIcon(
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
                modifier = Modifier
                    .fillMaxSize()
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

@Composable
private fun AchievementDateRow(
    achievedAtLabel: String,
    sourceDateLabel: String,
    occurrenceCount: Int,
    xpEarned: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = achievedAtLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Source date: $sourceDateLabel",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Times achieved: $occurrenceCount",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "XP earned: $xpEarned",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GoalDialog(
    currentGoalMinutes: Int,
    goalInput: String,
    onGoalInputChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var showError by remember { mutableStateOf(false) }
    val parsedGoal = goalInput.toIntOrNull()
    val canConfirm = parsedGoal != null && parsedGoal > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (currentGoalMinutes > 0) {
                    "Change screen time goal"
                } else {
                    "Set screen time goal"
                }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Enter your daily screen time goal in minutes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = goalInput,
                    onValueChange = {
                        showError = false
                        onGoalInputChange(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Goal (minutes)") },
                    isError = showError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                if (showError) {
                    Text(
                        text = "Enter a value greater than zero.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (parsedGoal != null && parsedGoal > 0) {
                        onConfirm(parsedGoal)
                    } else {
                        showError = true
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
