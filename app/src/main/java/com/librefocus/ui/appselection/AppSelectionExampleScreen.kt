package com.librefocus.ui.appselection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionExampleScreen() {
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedApps by remember { mutableStateOf<List<String>>(emptyList()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            Text("Selected ${selectedApps.size} apps")

            Button(onClick = { showBottomSheet = true }) {
                Text("Select Apps (Multi-select with Categories)")
            }

            Button(onClick = { showBottomSheet = true }) {
                Text("Select Apps (Single-select)")
            }

            Button(onClick = { showBottomSheet = true }) {
                Text("Select Apps (No Categories)")
            }
        }
    }

    if (showBottomSheet) {
        AppSelectionBottomSheet(
            onDismiss = {
                scope.launch {
                    sheetState.hide()
                }.invokeOnCompletion {
                    showBottomSheet = false
                }
            },
            onConfirm = { packages ->
                selectedApps = packages
                scope.launch {
                    sheetState.hide()
                }.invokeOnCompletion {
                    showBottomSheet = false
                }
            },
            showCategories = true,
            showPackageName = false,
            allowMultipleSelection = true,
            preSelectedPackages = selectedApps.toSet(),
            sheetState = sheetState
        )
    }
}
