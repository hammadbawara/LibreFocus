package com.librefocus.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.BottomAppBarScrollBehavior
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.navigation.NavController
import com.librefocus.ui.navigation.Screen

/**
 * Common scaffold component for the LibreFocus app.
 * Provides a consistent layout with optional TopAppBar and BottomNavigationBar.
 *
 * @param modifier Optional modifier for the scaffold
 * @param topBar Optional top app bar composable. If null, no top bar is shown
 * @param bottomBar Optional bottom navigation bar composable. If null, no bottom bar is shown
 * @param floatingActionButton Optional floating action button
 * @param floatingActionButtonPosition Position of the FAB (default: End)
 * @param snackbarHost Optional snackbar host. If null, a default one is provided
 * @param topAppBarScrollBehavior Optional scroll behavior for the top app bar
 * @param bottomBarScrollBehavior Optional scroll behavior for the bottom bar
 * @param showBottomBar Whether to show the bottom navigation bar (default: true)
 * @param contentWindowInsets Window insets for the content
 * @param content The main content of the screen. Receives paddingValues and scroll connection modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    snackbarHost: @Composable () -> Unit = { SnackbarHost(hostState = remember { SnackbarHostState() }) },
    topAppBarScrollBehavior: TopAppBarScrollBehavior? = null,
    bottomBarScrollBehavior: BottomAppBarScrollBehavior? = null,
    showBottomBar: Boolean = true,
    contentWindowInsets: WindowInsets = WindowInsets(0, 0, 0, 0),
    content: @Composable (PaddingValues, Modifier) -> Unit
) {
    // Collapse the top app bar by default
    LaunchedEffect(topAppBarScrollBehavior) {
        topAppBarScrollBehavior?.state?.apply {
            heightOffset = heightOffsetLimit
        }
    }
    
    // Create a modifier that combines both scroll behaviors
    val scrollModifier: Modifier = when {
        topAppBarScrollBehavior != null && bottomBarScrollBehavior != null ->
            Modifier
                .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                .nestedScroll(bottomBarScrollBehavior.nestedScrollConnection)
        topAppBarScrollBehavior != null ->
            Modifier.nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
        bottomBarScrollBehavior != null ->
            Modifier.nestedScroll(bottomBarScrollBehavior.nestedScrollConnection)
        else -> Modifier
    }
    
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = if (showBottomBar) bottomBar else { {} },
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        snackbarHost = snackbarHost,
        contentWindowInsets = contentWindowInsets
    ) { paddingValues ->
        content(paddingValues, scrollModifier)
    }
}

/**
 * Bottom navigation bar with scroll behavior support.
 * Should be used as the bottomBar parameter in AppScaffold.
 *
 * @param navController The navigation controller for handling navigation
 * @param currentRoute The currently selected route
 * @param scrollBehavior Optional scroll behavior for the bottom bar (default: exitAlwaysScrollBehavior)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomNavigationBar(
    navController: NavController,
    currentRoute: String?,
    scrollBehavior: BottomAppBarScrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()
) {
    val items = Screen.entries

    BottomAppBar(
        scrollBehavior = scrollBehavior
    ) {
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
                    icon = {
                        if (currentRoute == destination.route) {
                            destination.selectedIcon()
                        } else {
                            destination.unselectedIcon()
                        }
                    },
                    label = { Text(text = destination.label) }
                )
            }
        }
    }
}
