package com.librefocus.ui.limits

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.librefocus.ui.common.AppBottomNavigationBar
import com.librefocus.ui.common.AppScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LimitsScreen(
    navController: NavController,
    currentRoute: String?,
    modifier: Modifier = Modifier
) {
    val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val bottomBarScrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()
    
    AppScaffold(
        modifier = modifier,
        topBar = {
            LargeTopAppBar(
                title = { Text("Limits") },
                scrollBehavior = topAppBarScrollBehavior
            )
        },
        bottomBar = {
            AppBottomNavigationBar(
                navController = navController,
                currentRoute = currentRoute,
                scrollBehavior = bottomBarScrollBehavior
            )
        },
        topAppBarScrollBehavior = topAppBarScrollBehavior,
        bottomBarScrollBehavior = bottomBarScrollBehavior
    ) { paddingValues, scrollModifier ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .then(scrollModifier),
            contentAlignment = Alignment.Center
        ) {
            Text("Limits Screen")
        }
    }
}