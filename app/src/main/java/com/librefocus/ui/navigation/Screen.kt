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
import com.librefocus.R

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