package com.librefocus.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Scaffold { innerPadding ->
        Box(modifier = modifier.padding(innerPadding)) {
            Text("Home Screen")
        }
    }
}