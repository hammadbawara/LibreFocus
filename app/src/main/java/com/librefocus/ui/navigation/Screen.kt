package com.librefocus.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.librefocus.R
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

enum class Screen(
    val route: String,
    val label: String,
    val selectedIcon: @Composable () -> Unit,
    val unselectedIcon: @Composable () -> Unit
) {
    Home(
        route = "home",
        label = "Home",
        selectedIcon = {
            Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = null
            )
        },
        unselectedIcon = {
            Icon(
                imageVector = Icons.Outlined.Home,
                contentDescription = null
            )
        }
    ),
    Stats(
        route = "stats",
        label = "Stats",
        selectedIcon = {
            Icon(
                painter = painterResource(
                    id = R.drawable.ic_graph_filled),
                contentDescription = null
            )
        },
        unselectedIcon = {
            Icon(
                painter = painterResource(
                    id = R.drawable.ic_graph_outlined),
                contentDescription = null
            )
        }
    ),

    Limits (
        route = "limits",
        label = "Limits",
        selectedIcon = {
            Icon(
                imageVector = Icons.Filled.Block,
                contentDescription = null
            )
        },
        unselectedIcon = {
            Icon(
                imageVector = Icons.Outlined.Block,
                contentDescription = null
            )
        }
    ),

    Categories(
        route = "categories",
        label = "Categories",
        selectedIcon = {
            Icon(
                imageVector = Icons.Filled.Category,
                contentDescription = null
            )
        },
        unselectedIcon = {
            Icon(
                imageVector = Icons.Outlined.Category,
                contentDescription = null
            )
        }
    ),

    Settings (
        route = "settings",
        label = "Settings",
        selectedIcon = {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = null
            )
        },
        unselectedIcon = {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = null
            )
        }
    );


    companion object {
        val entries: List<Screen> = Screen.entries
    }
}

/**
 * Navigation routes for screens that require parameters.
 * These are not part of the bottom navigation.
 */
object AppDetailRoute {
    const val ROUTE_PATTERN = "app_detail/{packageName}/{appName}"
    
    val arguments: List<NamedNavArgument> = listOf(
        navArgument("packageName") { type = NavType.StringType },
        navArgument("appName") { type = NavType.StringType }
    )
    
    fun createRoute(packageName: String, appName: String): String {
        val encodedPackage = URLEncoder.encode(packageName, StandardCharsets.UTF_8.toString())
        val encodedName = URLEncoder.encode(appName, StandardCharsets.UTF_8.toString())
        return "app_detail/$encodedPackage/$encodedName"
    }
}