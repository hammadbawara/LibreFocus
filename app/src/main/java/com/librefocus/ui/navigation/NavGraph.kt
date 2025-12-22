package com.librefocus.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.librefocus.ui.home.HomeScreen
import com.librefocus.ui.limits.LimitsScreen
import com.librefocus.ui.settings.SettingsScreen
import com.librefocus.ui.stats.AppDetailScreen
import com.librefocus.ui.stats.StatsScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

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
                onAppClick = { packageName ->
                    navController.navigate("app_detail/$packageName")
                }
            )
        }
        composable(Screen.Limits.route) {
            LimitsScreen(
                navController = navController,
                currentRoute = currentRoute
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
            route = "app_detail/{packageName}",
            arguments = listOf(
                navArgument("packageName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName") ?: ""
            AppDetailScreen(
                packageName = packageName,
                onBackClick = { navController.navigateUp() }
            )
        }
    }
}