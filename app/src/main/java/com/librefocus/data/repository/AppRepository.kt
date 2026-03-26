package com.librefocus.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.librefocus.data.local.AppInfoProvider
import com.librefocus.models.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for managing installed app information.
 * Handles fetching app details from the system and checking installation status.
 */
class AppRepository(
    private val context: Context,
    private val appInfoProvider: AppInfoProvider
) {

    /**
     * Fetches all installed user apps (non-system apps).
     * 
     * @return List of installed apps with their details
     */
    suspend fun getInstalledApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        packages
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .mapNotNull { appInfo ->
                try {
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val icon = packageManager.getApplicationIcon(appInfo)
                    val category = appInfoProvider.getAppCategory(appInfo.packageName)

                    InstalledApp(
                        packageName = appInfo.packageName,
                        appName = appName,
                        icon = icon,
                        category = category
                    )
                } catch (e: Exception) {
                    null
                }
            }
            .sortedBy { it.appName.lowercase() }
    }

    /**
     * Checks if an app is installed on the device.
     * 
     * @param packageName The package name to check
     * @return true if app is installed, false otherwise
     */
    suspend fun isAppInstalled(packageName: String): Boolean {
        return appInfoProvider.isAppInstalled(packageName)
    }

    /**
     * Gets the app name for a given package name.
     * 
     * @param packageName The package name
     * @return App name or package name if not found
     */
    suspend fun getAppName(packageName: String): String {
        return appInfoProvider.getAppName(packageName)
    }

    /**
     * Gets the app icon for a given package name.
     * 
     * @param packageName The package name
     * @return App icon drawable or null if not found
     */
    suspend fun getAppIcon(packageName: String): Drawable? = withContext(Dispatchers.IO) {
        try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
}
