package com.librefocus.data.repository

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import com.librefocus.models.AppUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class UsageRepository(
    private val context: Context,
    private val usageStatsManager: UsageStatsManager
) {

    suspend fun getTodayAppUsage(): List<AppUsage> = withContext(Dispatchers.IO) {
        val endTime = System.currentTimeMillis()
        val startTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: return@withContext emptyList()

        val packageManager = context.packageManager

        stats.filter { it.totalTimeInForeground > 0 }
            .mapNotNull { usageStat ->
                try {
                    val appInfo = packageManager.getApplicationInfo(usageStat.packageName, 0)
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val icon = packageManager.getApplicationIcon(appInfo)
                    AppUsage(
                        packageName = usageStat.packageName,
                        appName = appName,
                        icon = icon,
                        usageTimeMillis = usageStat.totalTimeInForeground
                    )
                } catch (_: PackageManager.NameNotFoundException) {
                    null
                }
            }
            .sortedByDescending { it.usageTimeMillis }
    }
}
