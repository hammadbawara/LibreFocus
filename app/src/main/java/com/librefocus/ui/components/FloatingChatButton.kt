package com.librefocus.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun FloatingChatButton(navController: NavController, modifier: Modifier = Modifier, alignEnd: Boolean = true) {
    Box(modifier = modifier.fillMaxSize()) {
        val alignment = if (alignEnd) Alignment.BottomEnd else Alignment.BottomStart

        // Compute bottom inset coming from system navigation bars so we can position the FAB above it
        val navBarBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        // App bottom navigation height approximation (match Limits FAB placement)
        val appBottomNavHeight = 84.dp

        // Small extra gap above the app bottom navigation (copying the visual offset used in AppScaffold FABs)
        val appBottomNavGap = 12.dp

        FloatingActionButton(
            onClick = { navController.navigate("chatbot") },
            modifier = Modifier
                // Gap above nav bar + app bottom navigation height + small gap so FAB doesn't overlap app nav
                .padding(
                    bottom = navBarBottomInset + appBottomNavHeight + appBottomNavGap,
                    end = if (alignEnd) 15.dp else 0.dp,
                    start = if (!alignEnd) 24.dp else 0.dp
                )
                .align(alignment)
        ) {
            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat")
        }
    }
}
