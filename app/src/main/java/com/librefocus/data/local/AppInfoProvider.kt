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
    /**
     * Fetches app metadata (name) from package manager.
     *
     * @param packageName The package name of the app
     * @return App name or the package name if not found
     */
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

    suspend fun getAppCategory(packageName: String): String = withContext(Dispatchers.IO) {
        val  pm = context.packageManager
        val applicationInfo = pm.getApplicationInfo(packageName, 0);
        val appCategory = applicationInfo.category
        val categoryTitle = ApplicationInfo.getCategoryTitle(context, appCategory)
        return@withContext categoryTitle.toString()
    }
}

