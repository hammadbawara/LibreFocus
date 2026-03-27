package com.librefocus.ui.limits

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.librefocus.utils.PermissionUtils

@Composable
fun LimitsPermissionDialog(
    onDismissRequest: () -> Unit,
    onPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    var hasAccessibility by remember { mutableStateOf(PermissionUtils.isAccessibilityServiceEnabled(context)) }
    var hasDrawOverlays by remember { mutableStateOf(PermissionUtils.canDrawOverlays(context)) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasAccessibility = PermissionUtils.isAccessibilityServiceEnabled(context)
        hasDrawOverlays = PermissionUtils.canDrawOverlays(context)
        
        if (hasAccessibility && hasDrawOverlays) {
            onPermissionsGranted()
        }
    }

    if (hasAccessibility && hasDrawOverlays) {
        return
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Permissions Required") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("To limit app usage, LibreFocus needs the following permissions to work correctly:")
                
                if (!hasAccessibility) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Accessibility Service: Needed to block distracting apps based on your limits.")
                    Button(
                        onClick = {
                            PermissionUtils.showPermissionToast(context)
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grant Accessibility")
                    }
                }
                
                if (!hasDrawOverlays) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Draw over other apps: Needed to show the blocking screen over apps when limits are reached.")
                    Button(
                        onClick = {
                            PermissionUtils.showPermissionToast(context)
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grant Overlay")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    hasAccessibility = PermissionUtils.isAccessibilityServiceEnabled(context)
                    hasDrawOverlays = PermissionUtils.canDrawOverlays(context)
                    if (hasAccessibility && hasDrawOverlays) {
                        onPermissionsGranted()
                    }
                }
            ) {
                Text("I've granted them")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}
