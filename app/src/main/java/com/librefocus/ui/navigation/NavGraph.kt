package com.librefocus.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.librefocus.ui.categories.CategoryScreen
import com.librefocus.ui.chatbot.ChatbotScreen
import com.librefocus.ui.components.FloatingChatButton
import com.librefocus.ui.home.HomeScreen
import com.librefocus.ui.limits.LimitsScreen
import com.librefocus.ui.settings.SettingsScreen
import com.librefocus.ui.stats.AppDetailScreen
import com.librefocus.ui.stats.StatsScreen
import org.koin.androidx.compose.koinViewModel
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Box(modifier = Modifier.fillMaxSize()) {
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
            composable(Screen.Categories.route) {
                CategoryScreen(
                    onNavigateBack = {
                        navController.navigate(Screen.Settings.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
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
                val packageName = URLDecoder.decode(encodedPackageName, StandardCharsets.UTF_8.toString())
                AppDetailScreen(
                    packageName = packageName,
                    onBackClick = { navController.navigateUp() }
                )
            }
            composable("chatbot") {
                ChatbotScreen(
                    navController = navController,
                    viewModel = koinViewModel()
                )
            }
        }

        if (currentRoute != "chatbot") {
            FloatingChatButton(
                navController = navController,
                modifier = Modifier.padding(bottom = 80.dp) // Add padding to move the button up
            )
        }
    }
}