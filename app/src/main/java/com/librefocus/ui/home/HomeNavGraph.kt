package com.librefocus.ui.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.librefocus.ui.stats.StatsScreen

@Composable
fun HomeNavGraph() {
    val navController = rememberNavController()
    val items = HomeDestination.entries
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    androidx.compose.material3.Scaffold(
        bottomBar = {
            NavigationBar(tonalElevation = 0.dp) {
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
            startDestination = HomeDestination.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(HomeDestination.Home.route) {
                HomeScreen()
            }
            composable(HomeDestination.Stats.route) {
                StatsScreen()
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
        icon = { androidx.compose.material3.Icon(imageVector = Icons.Outlined.Home, contentDescription = null) }
    ),
    Stats(
        route = "stats",
        label = "Stats",
        icon = { androidx.compose.material3.Icon(imageVector = Icons.Outlined.Insights, contentDescription = null) }
    );

    companion object {
        val entries: List<HomeDestination> = values().toList()
    }
}