package com.librefocus.data.local

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppInfoProvider(
    val context: Context
) {

    companion object {
        private const val TAG = "ApplicationInfoProvider"
    }

    suspend fun isAppInstalled(packageName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    suspend fun getAppName(packageName: String): String = withContext(Dispatchers.IO) {
        try {
            val packageManager = context.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "App name not found for package: $packageName")
            packageName
        }
    }

    /**
     * Fetches app category from package manager.
     *
     * @param packageName The package name of the app
     * @return Category name or "Uncategorized" if not found or error occurs
     */
    suspend fun getAppCategory(packageName: String): String = withContext(Dispatchers.IO) {
        try {
            val pm = context.packageManager
            val applicationInfo = pm.getApplicationInfo(packageName, 0)
            return@withContext getAppCategoryInternal(applicationInfo)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Category not found for package: $packageName", e)
            return@withContext "Uncategorized"
        } catch (e: Exception) {
            Log.w(TAG, "Error getting category for package: $packageName", e)
            return@withContext "Uncategorized"
        }
    }

    suspend fun getCategoryId(packageName: String): Int = withContext(Dispatchers.IO) {
        try {
            val pm = context.packageManager
            val applicationInfo = pm.getApplicationInfo(packageName, 0)
            return@withContext applicationInfo.category
        } catch (e: PackageManager.NameNotFoundException) {
            return@withContext ApplicationInfo.CATEGORY_UNDEFINED
        } catch (e: Exception) {
            return@withContext ApplicationInfo.CATEGORY_UNDEFINED
        }
    }

    /**
     * Data class to hold basic information about an installed app.
     */
    data class InstalledAppInfo(
        val packageName: String,
        val appName: String,
        val category: String
    )

    /**
     * Fetches all installed non-system apps.
     *
     * @return List of installed apps with their basic information
     */
    suspend fun getInstalledApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        try {
            val packageManager = context.packageManager
            val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            
            packages
                .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
                .mapNotNull { appInfo ->
                    try {
                        val appName = packageManager.getApplicationLabel(appInfo).toString()
                        val category = getAppCategoryInternal(appInfo)
                        
                        InstalledAppInfo(
                            packageName = appInfo.packageName,
                            appName = appName,
                            category = category
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Error getting info for package: ${appInfo.packageName}", e)
                        null
                    }
                }
                .sortedBy { it.appName.lowercase() }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching installed apps", e)
            emptyList()
        }
    }

    /**
     * Internal helper to get category from ApplicationInfo object.
     */
    private fun getAppCategoryInternal(applicationInfo: ApplicationInfo): String {
        return try {
            val appCategory = applicationInfo.category
            
            if (appCategory == ApplicationInfo.CATEGORY_UNDEFINED) {
                return "Uncategorized"
            }
            
            val categoryTitle = ApplicationInfo.getCategoryTitle(context, appCategory)
            categoryTitle?.toString() ?: "Uncategorized"
        } catch (e: Exception) {
            Log.w(TAG, "Error getting category for app", e)
            "Uncategorized"
        }
    }
}

