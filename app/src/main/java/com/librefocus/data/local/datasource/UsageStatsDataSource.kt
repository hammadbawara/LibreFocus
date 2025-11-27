package com.librefocus.data.local.datasource

import android.app.usage.UsageEvents
import android.app.usage.UsageEventsQuery
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.librefocus.models.AppUsageData
import com.librefocus.models.UsageEventData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Data source that wraps Android's UsageStatsManager API.
 * Fetches raw usage statistics from the system and normalizes them into domain models.
 */
class UsageStatsDataSource(
    private val context: Context,
    private val usageStatsManager: UsageStatsManager
) {
    
    companion object {
        private const val TAG = "UsageStatsDataSource"
    }
    
    /**
     * Fetches usage events from the system for the specified time range.
     * Returns a list of usage events containing app activity information.
     * 
     * @param startTimeUtc Start time in UTC epoch milliseconds
     * @param endTimeUtc End time in UTC epoch milliseconds
     * @return List of usage event data
     */
    suspend fun fetchUsageEvents(
        startTimeUtc: Long,
        endTimeUtc: Long
    ): List<UsageEventData> = withContext(Dispatchers.IO) {
        val events = mutableListOf<UsageEventData>()

        try {
            val query = UsageEventsQuery.Builder(startTimeUtc, endTimeUtc)
                .setEventTypes(
                    UsageEvents.Event.ACTIVITY_RESUMED,
                    UsageEvents.Event.ACTIVITY_PAUSED,
                    //UsageEvents.Event.ACTIVITY_STOPPED
                )
                .build()

            val usageEvents = usageStatsManager.queryEvents(query)
            val event = UsageEvents.Event()

            usageEvents?.let { usageEvents->
                while (usageEvents.hasNextEvent()) {
                    usageEvents.getNextEvent(event)

                    event.packageName?.let { packageName ->
                        events.add(
                            UsageEventData(
                                packageName = packageName,
                                timestampUtc = event.timeStamp,
                                eventType = event.eventType
                            )
                        )
                    }
                }
            }


        } catch (e: Exception) {
            Log.e("UsageEvents", "Error fetching usage events", e)
        }

        events
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
    
    /**
     * Checks if an app is currently installed on the device.
     * 
     * @param packageName The package name to check
     * @return true if app is installed, false otherwise
     */
    suspend fun isAppInstalled(packageName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            context.packageManager.getApplicationInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    /**
     * Fetches all installed apps on the device.
     * 
     * @return List of app usage data with package names and app names
     */
    suspend fun fetchInstalledApps(): List<AppUsageData> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val installedApps = packageManager.getInstalledApplications(0)
        
        installedApps.mapNotNull { appInfo ->
            try {
                AppUsageData(
                    packageName = appInfo.packageName,
                    appName = packageManager.getApplicationLabel(appInfo).toString(),
                    usageDurationMillis = 0,
                    launchCount = 0
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error getting app info for: ${appInfo.packageName}", e)
                null
            }
        }
    }
}
