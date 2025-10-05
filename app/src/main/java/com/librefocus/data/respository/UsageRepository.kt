package com.librefocus.data.repository

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // First, build durations based on UsageEvents (foreground/background pairs)
        val eventDurations = mutableMapOf<String, Long>()
        val currentStart = mutableMapOf<String, Long>()

        val usageEvents: UsageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            val has = usageEvents.getNextEvent(event)
            if (!has) continue
            val pkg = event.packageName ?: continue
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    // record start time if not already started
                    if (!currentStart.containsKey(pkg)) {
                        currentStart[pkg] = event.timeStamp
                    }
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    val start = currentStart.remove(pkg)
                    if (start != null && event.timeStamp > start) {
                        val delta = event.timeStamp - start
                        eventDurations[pkg] = (eventDurations[pkg] ?: 0L) + delta
                    }
                }
                else -> {
                    // ignore other event types
                }
            }
        }

        // If some apps still have a start without a background, count until endTime
        for ((pkg, start) in currentStart) {
            if (endTime > start) {
                eventDurations[pkg] = (eventDurations[pkg] ?: 0L) + (endTime - start)
            }
        }

        // Also query aggregated stats to include packages that might not have events
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime) ?: emptyList()
        val statsMap = stats.associateBy { it.packageName }

        // Merge keys and prefer event-based durations when available
        val allPackages = (eventDurations.keys + statsMap.keys)

        val packageManager = context.packageManager

        val result = allPackages.mapNotNull { pkg ->
            // Prefer event-based duration since it directly measures foreground/background pairs
            val fallbackMillis = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                statsMap[pkg]?.totalTimeVisible ?: statsMap[pkg]?.totalTimeInForeground ?: 0L
            } else {
                statsMap[pkg]?.totalTimeInForeground ?: 0L
            }
            val visibleMillis = eventDurations[pkg] ?: fallbackMillis
            if (visibleMillis <= 0L) return@mapNotNull null
            try {
                val appInfo = packageManager.getApplicationInfo(pkg, 0)
                val appName = packageManager.getApplicationLabel(appInfo).toString()
                val icon = packageManager.getApplicationIcon(appInfo)
                AppUsage(
                    packageName = pkg,
                    appName = appName,
                    icon = icon,
                    usageTimeMillis = visibleMillis
                )
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
        }
            .sortedByDescending { it.usageTimeMillis }

        result
    }
}
