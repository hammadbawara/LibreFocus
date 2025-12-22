package com.librefocus.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
    val items = Screen.entries
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            if (currentRoute != destination.route) {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { if (currentRoute == destination.route) destination.selectedIcon() else destination.unselectedIcon() },
                        label = { Text(text = destination.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Stats.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen()
            }
            composable(Screen.Stats.route) {
                StatsScreen(
                    onAppClick = { packageName ->
                        navController.navigate("app_detail/$packageName")
                    }
                )
            }
            composable(Screen.Limits.route) {
                LimitsScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
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
}