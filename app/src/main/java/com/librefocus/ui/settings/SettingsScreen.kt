package com.librefocus.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsMenuLink
import com.alorma.compose.settings.ui.SettingsSwitch
import com.librefocus.models.DateFormat
import com.librefocus.models.TimeFormat
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onBackClick: () -> Unit
) {
    val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val dateTimePrefs by viewModel.dateTimePreferences.collectAsStateWithLifecycle()
    val formattedPrefs by viewModel.formattedPreferences.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showThemeDialog by remember { mutableStateOf(false) }
    var showTimeFormatDialog by remember { mutableStateOf(false) }
    var showDateFormatDialog by remember { mutableStateOf(false) }
    var showTimeZoneDialog by remember { mutableStateOf(false) }

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
            }
            
            SettingsGroup(
                title = { Text("Date & Time") }
            ) {
                SettingsSwitch(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null
                        )
                    },
                    title = { Text("Use System Date & Time") },
                    subtitle = { Text("Automatically use system defaults for format and timezone") },
                    state = dateTimePrefs.useSystemDefaults,
                    onCheckedChange = { viewModel.setUseSystemDefaults(it) }
                )
                
                if (!dateTimePrefs.useSystemDefaults) {
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
                                when (dateTimePrefs.timeFormat) {
                                    TimeFormat.SYSTEM -> "System Default"
                                    TimeFormat.TWELVE_HOUR -> "12-hour"
                                    TimeFormat.TWENTY_FOUR_HOUR -> "24-hour"
                                }
                            )
                        },
                        onClick = { showTimeFormatDialog = true }
                    )
                    
                    SettingsMenuLink(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null
                            )
                        },
                        title = { Text("Date Format") },
                        subtitle = {
                            Text(
                                when (dateTimePrefs.dateFormat) {
                                    DateFormat.SYSTEM -> "System Default"
                                    DateFormat.DD_MMM_YYYY -> "21 Dec 2025"
                                    DateFormat.MM_DD_YYYY -> "12/21/2025"
                                    DateFormat.YYYY_MM_DD -> "2025-12-21"
                                }
                            )
                        },
                        onClick = { showDateFormatDialog = true }
                    )
                    
                    SettingsMenuLink(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null
                            )
                        },
                        title = { Text("Time Zone") },
                        subtitle = {
                            Text(dateTimePrefs.getEffectiveTimeZoneId())
                        },
                        onClick = { showTimeZoneDialog = true }
                    )
                }
                
                // Live preview card
                formattedPrefs?.let { formatted ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Preview",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val now = System.currentTimeMillis()
                            Text(
                                text = "Date: ${formatted.formatDate(now)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Time: ${formatted.formatTime(now)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Date & Time: ${formatted.formatDateTime(now)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
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
                        val intent = Intent(Intent.ACTION_VIEW,
                            "https://github.com/hammadbawara/LibreFocus".toUri())
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
                        text = "System Default",
                        selected = dateTimePrefs.timeFormat == TimeFormat.SYSTEM,
                        onClick = {
                            viewModel.setTimeFormat(TimeFormat.SYSTEM)
                            showTimeFormatDialog = false
                        }
                    )
                    ThemeOption(
                        text = "12-hour",
                        selected = dateTimePrefs.timeFormat == TimeFormat.TWELVE_HOUR,
                        onClick = {
                            viewModel.setTimeFormat(TimeFormat.TWELVE_HOUR)
                            showTimeFormatDialog = false
                        }
                    )
                    ThemeOption(
                        text = "24-hour",
                        selected = dateTimePrefs.timeFormat == TimeFormat.TWENTY_FOUR_HOUR,
                        onClick = {
                            viewModel.setTimeFormat(TimeFormat.TWENTY_FOUR_HOUR)
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
    
    if (showDateFormatDialog) {
        AlertDialog(
            onDismissRequest = { showDateFormatDialog = false },
            title = { Text("Date Format") },
            text = {
                Column {
                    DateFormatOption(
                        text = "System Default",
                        selected = dateTimePrefs.dateFormat == DateFormat.SYSTEM,
                        onClick = {
                            viewModel.setDateFormat(DateFormat.SYSTEM)
                            showDateFormatDialog = false
                        }
                    )
                    DateFormatOption(
                        text = "21 Dec 2025",
                        selected = dateTimePrefs.dateFormat == DateFormat.DD_MMM_YYYY,
                        onClick = {
                            viewModel.setDateFormat(DateFormat.DD_MMM_YYYY)
                            showDateFormatDialog = false
                        }
                    )
                    DateFormatOption(
                        text = "12/21/2025",
                        selected = dateTimePrefs.dateFormat == DateFormat.MM_DD_YYYY,
                        onClick = {
                            viewModel.setDateFormat(DateFormat.MM_DD_YYYY)
                            showDateFormatDialog = false
                        }
                    )
                    DateFormatOption(
                        text = "2025-12-21",
                        selected = dateTimePrefs.dateFormat == DateFormat.YYYY_MM_DD,
                        onClick = {
                            viewModel.setDateFormat(DateFormat.YYYY_MM_DD)
                            showDateFormatDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDateFormatDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    if (showTimeZoneDialog) {
        AlertDialog(
            onDismissRequest = { showTimeZoneDialog = false },
            title = { Text("Time Zone") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    TimeZoneOption(
                        text = "System Default",
                        selected = dateTimePrefs.timeZoneId.isNullOrEmpty() || dateTimePrefs.timeZoneId == "SYSTEM",
                        onClick = {
                            viewModel.setTimeZone(null)
                            showTimeZoneDialog = false
                        }
                    )
                    
                    // Show common time zones
                    val commonZones = listOf(
                        "UTC",
                        "America/New_York",
                        "America/Chicago",
                        "America/Denver",
                        "America/Los_Angeles",
                        "Europe/London",
                        "Europe/Paris",
                        "Europe/Berlin",
                        "Asia/Tokyo",
                        "Asia/Shanghai",
                        "Asia/Kolkata",
                        "Australia/Sydney"
                    )
                    
                    commonZones.forEach { zoneId ->
                        TimeZoneOption(
                            text = zoneId.replace("_", " "),
                            selected = dateTimePrefs.timeZoneId == zoneId,
                            onClick = {
                                viewModel.setTimeZone(zoneId)
                                showTimeZoneDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimeZoneDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DateFormatOption(
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
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun TimeZoneOption(
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
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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