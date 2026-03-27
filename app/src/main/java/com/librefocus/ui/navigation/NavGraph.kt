package com.librefocus.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.librefocus.ui.categories.CategoryScreen
import com.librefocus.ui.chatbot.ChatbotScreen
import com.librefocus.ui.components.FloatingChatButton
import com.librefocus.ui.home.HomeScreen
import com.librefocus.ui.limits.CreateLimitScreen
import com.librefocus.ui.limits.LaunchCountLimitScreen
import com.librefocus.ui.limits.LimitConfiguration
import com.librefocus.ui.limits.LimitsScreen
import com.librefocus.ui.limits.ScheduleLimitScreen
import com.librefocus.ui.limits.SetLimitScreen
import com.librefocus.ui.limits.UsageLimitScreen
import com.librefocus.ui.settings.SettingsScreen
import com.librefocus.ui.stats.AppDetailScreen
import com.librefocus.ui.stats.StatsScreen
import com.librefocus.data.IApiKeyProvider
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun NavGraph(apiKeyProvider: IApiKeyProvider) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route


    Box {
        NavHost(
            navController = navController,
            startDestination = Screen.Stats.route
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    navController = navController,
                    currentRoute = currentRoute
                )
            }
            composable(Screen.Stats.route) {
                StatsScreen(
                    navController = navController,
                    currentRoute = currentRoute,
                    onAppClick = { packageName, appName ->
                        navController.navigate(AppDetailRoute.createRoute(packageName, appName))
                    }
                )
            }
            composable(Screen.Limits.route) {
                LimitsScreen(
                    navController = navController,
                    currentRoute = currentRoute
                )
            }
            composable("create_limit") {
                val savedStateHandle = it.savedStateHandle
                val limitConfigResult by savedStateHandle.getStateFlow<LimitConfiguration?>(
                    "limit_config_result",
                    null
                )
                    .collectAsStateWithLifecycle(initialValue = null)

                CreateLimitScreen(
                    onNavigateBack = {
                        navController.navigateUp()
                    },
                    onNavigateToSetLimit = {
                        navController.navigate("set_limit")
                    },
                    limitConfigResult = limitConfigResult
                )
            }
            composable(
                route = "create_limit/{limitId}",
                arguments = listOf(
                    navArgument("limitId") { type = NavType.StringType }
                )
            ) {
                val savedStateHandle = it.savedStateHandle
                val limitConfigResult by savedStateHandle.getStateFlow<LimitConfiguration?>(
                    "limit_config_result",
                    null
                )
                    .collectAsStateWithLifecycle(initialValue = null)

                CreateLimitScreen(
                    onNavigateBack = {
                        navController.navigateUp()
                    },
                    onNavigateToSetLimit = {
                        navController.navigate("set_limit")
                    },
                    limitConfigResult = limitConfigResult
                )
            }
            composable("set_limit") {
                // Get the previous back stack entry (either create_limit or create_limit/{limitId})
                val createLimitEntry = navController.previousBackStackEntry

                // Observe result from limit config screens
                LaunchedEffect(Unit) {
                    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
                    savedStateHandle?.getStateFlow<LimitConfiguration?>("limit_config_result", null)
                        ?.collect { config ->
                            if (config != null) {
                                createLimitEntry?.savedStateHandle?.set(
                                    "limit_config_result",
                                    config
                                )
                                savedStateHandle.remove<LimitConfiguration?>("limit_config_result")
                                navController.navigateUp()
                            }
                        }
                }

                SetLimitScreen(
                    onNavigateBack = {
                        navController.navigateUp()
                    },
                    onNavigateToSchedule = {
                        navController.navigate("schedule_limit")
                    },
                    onNavigateToUsage = {
                        navController.navigate("usage_limit")
                    },
                    onNavigateToLaunchCount = {
                        navController.navigate("launch_count_limit")
                    }
                )
            }
            composable("schedule_limit") {
                ScheduleLimitScreen(
                    onNavigateBack = { config ->
                        if (config != null) {
                            navController.previousBackStackEntry?.savedStateHandle?.set(
                                "limit_config_result",
                                config
                            )
                        }
                        navController.navigateUp()
                    }
                )
            }
            composable("usage_limit") {
                UsageLimitScreen(
                    onNavigateBack = { config ->
                        if (config != null) {
                            navController.previousBackStackEntry?.savedStateHandle?.set(
                                "limit_config_result",
                                config
                            )
                        }
                        navController.navigateUp()
                    }
                )
            }
            composable("launch_count_limit") {
                LaunchCountLimitScreen(
                    onNavigateBack = { config ->
                        if (config != null) {
                            navController.previousBackStackEntry?.savedStateHandle?.set(
                                "limit_config_result",
                                config
                            )
                        }
                        navController.navigateUp()
                    }
                )
            }
            composable("categories") {
                CategoryScreen(
                    onNavigateBack = {
                        navController.navigateUp()
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    navController = navController,
                    currentRoute = currentRoute,
                    onBackClick = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(
                route = AppDetailRoute.ROUTE_PATTERN,
                arguments = AppDetailRoute.arguments
            ) { backStackEntry ->
                val encodedPackageName = backStackEntry.arguments?.getString("packageName") ?: ""
                val packageName =
                    URLDecoder.decode(encodedPackageName, StandardCharsets.UTF_8.toString())
                AppDetailScreen(
                    packageName = packageName,
                    onBackClick = { navController.navigateUp() }
                )
            }

            composable(
                route = "chatbot"
            ) {
                ChatbotScreen(navController, apiKeyProvider = apiKeyProvider)
            }
        }


        // Routes that define their own FAB; chat FAB should be hidden on these routes to avoid overlap
        val routesWithOwnFab = listOf(
            Screen.Limits.route, // "limits"
            "categories",
            "create_limit",
            "set_limit",
            "schedule_limit",
            "usage_limit",
            "launch_count_limit",
            Screen.Settings.route
        )

        // Only show the global chat FAB when we're not on the chatbot screen and the current route
        // is known and does not already provide a FAB (so we don't render two overlapping FABs).
        val shouldShowChatFab = currentRoute != null && currentRoute != "chatbot" && routesWithOwnFab.none { route ->
            currentRoute == route || currentRoute.startsWith(route)
        }

        if (shouldShowChatFab) {
            FloatingChatButton(
                navController
            )
        }

    }
}