package com.librefocus.utils

import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import com.librefocus.services.AppBlockingService

object PermissionUtils {
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponentName = "${context.packageName}/${AppBlockingService::class.java.canonicalName}"
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val stringifier = TextUtils.SimpleStringSplitter(':')
        stringifier.setString(enabledServicesSetting)

        while (stringifier.hasNext()) {
            val componentName = stringifier.next()
            if (componentName.equals(expectedComponentName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun showPermissionToast(context: Context) {
        Toast.makeText(context, "Select LibreFocus and give permission", Toast.LENGTH_LONG).show()
    }
}
