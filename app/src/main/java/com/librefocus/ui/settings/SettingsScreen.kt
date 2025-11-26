package com.librefocus.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsMenuLink
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onBackClick: () -> Unit
) {
    val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val timeFormat by viewModel.timeFormat.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showThemeDialog by remember { mutableStateOf(false) }
    var showTimeFormatDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
//                navigationIcon = {
//                    IconButton(onClick = onBackClick) {
//                        Icon(
//                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
//                            contentDescription = "Back"
//                        )
//                    }
//                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsGroup(
                title = { Text("Appearance") }
            ) {
                SettingsMenuLink(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.BrightnessMedium,
                            contentDescription = null
                        )
                    },
                    title = { Text("App Theme") },
                    subtitle = {
                        Text(
                            when (appTheme) {
                                "LIGHT" -> "Light"
                                "DARK" -> "Dark"
                                else -> "System Default"
                            }
                        )
                    },
                    onClick = { showThemeDialog = true }
                )

                SettingsMenuLink(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null
                        )
                    },
                    title = { Text("Time Format") },
                    subtitle = {
                        Text(
                            when (timeFormat) {
                                "24H" -> "24-hour"
                                else -> "12-hour"
                            }
                        )
                    },
                    onClick = { showTimeFormatDialog = true }
                )
            }

            SettingsGroup(
                title = { Text("About") }
            ) {
                SettingsMenuLink(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null
                        )
                    },
                    title = { Text("LibreFocus") },
                    subtitle = { Text("v1.0.0") },
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/LibreFocus/LibreFocus"))
                        context.startActivity(intent)
                    }
                )
            }
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme") },
            text = {
                Column {
                    ThemeOption(
                        text = "System Default",
                        selected = appTheme == "SYSTEM",
                        onClick = {
                            viewModel.setAppTheme("SYSTEM")
                            showThemeDialog = false
                        }
                    )
                    ThemeOption(
                        text = "Light",
                        selected = appTheme == "LIGHT",
                        onClick = {
                            viewModel.setAppTheme("LIGHT")
                            showThemeDialog = false
                        }
                    )
                    ThemeOption(
                        text = "Dark",
                        selected = appTheme == "DARK",
                        onClick = {
                            viewModel.setAppTheme("DARK")
                            showThemeDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showTimeFormatDialog) {
        AlertDialog(
            onDismissRequest = { showTimeFormatDialog = false },
            title = { Text("Time Format") },
            text = {
                Column {
                    ThemeOption(
                        text = "12-hour",
                        selected = timeFormat == "12H",
                        onClick = {
                            viewModel.setTimeFormat("12H")
                            showTimeFormatDialog = false
                        }
                    )
                    ThemeOption(
                        text = "24-hour",
                        selected = timeFormat == "24H",
                        onClick = {
                            viewModel.setTimeFormat("24H")
                            showTimeFormatDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimeFormatDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ThemeOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            color = if (selected) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.onSurface
        )
    }
}