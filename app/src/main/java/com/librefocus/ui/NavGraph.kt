package com.librefocus.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.librefocus.R
import com.librefocus.ui.home.HomeScreen
import com.librefocus.ui.limits.LimitsScreen
import com.librefocus.ui.settings.SettingsScreen
import com.librefocus.ui.stats.StatsScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val items = HomeDestination.entries
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
                        icon = destination.icon,
                        label = { Text(text = destination.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeDestination.Stats.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(HomeDestination.Home.route) {
                HomeScreen()
            }
            composable(HomeDestination.Stats.route) {
                StatsScreen()
            }
            composable(HomeDestination.Limits.route) {
                LimitsScreen()
            }
            composable(HomeDestination.Settings.route) {
                SettingsScreen(
                    onBackClick = {
                        navController.navigate(HomeDestination.Home.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}

private enum class HomeDestination(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit
) {
    Home(
        route = "home",
        label = "Home",
        icon = {
            Icon(
                imageVector = Icons.Outlined.Home,
                contentDescription = null
            )
        }
    ),
    Stats(
        route = "stats",
        label = "Stats",
        icon = {
            Icon(
                painter = painterResource(
                    id = R.drawable.ic_graph_filled),
                contentDescription = null
            )
        }
    ),

    Limits (
        route = "limits",
        label = "Limits",
        icon = {
            Icon(
                painter = painterResource(
                    id = R.drawable.ic_user_filled),
                contentDescription = null
            )
        }
    ),

    Settings (
        route = "settings",
        label = "Settings",
        icon = {
            Icon(
                imageVector = Icons.Outlined.Settings,
                    contentDescription = null
            )
        }
    );


    companion object {
        val entries: List<HomeDestination> = HomeDestination.entries
    }
}