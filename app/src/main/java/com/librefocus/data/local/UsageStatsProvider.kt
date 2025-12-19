package com.librefocus.data.local

import android.app.ActivityManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.collections.iterator

/**
 * Represents a continuous foreground session for an application.
 */
private data class AppForegroundSession(
    val packageName: String,
    val sessionStartTimeMillis: Long,
    val sessionEndTimeMillis: Long
)

/**
 * Internal representation of an app component (package + activity class).
 */
private data class AppComponent(
    val packageName: String,
    val activityClassName: String
)

/**
 * Manages application usage tracking and statistics calculation.
 *
 * This class processes Android UsageStats events to track app foreground time
 * and aggregates the data into hourly statistics.
 */
class UsageStatsProvider(
    private val context: Context,
    private val usageStatsManager: UsageStatsManager
) {
    // Time conversion constants
    private companion object {

        private const val TAG = "UsageStatsDataSource"
        const val MILLISECONDS_PER_SECOND = 1000L
        const val SECONDS_PER_MINUTE = 60L
        const val MINUTES_PER_HOUR = 60L

        const val MILLISECONDS_PER_MINUTE = SECONDS_PER_MINUTE * MILLISECONDS_PER_SECOND
        const val MILLISECONDS_PER_HOUR = MINUTES_PER_HOUR * MILLISECONDS_PER_MINUTE

        const val CURRENT_TIME_THRESHOLD_MILLIS = 1500L

        // UsageEvents.Event constants for older API levels
        const val EVENT_ACTIVITY_RESUMED_LEGACY = 4
        const val EVENT_ACTIVITY_STOPPED_LEGACY = 3
    }

    /**
     * Calculates the start of the hour for a given timestamp.
     */
    private fun calculateHourStartMillis(timestampMillis: Long): Long {
        return (timestampMillis / MILLISECONDS_PER_HOUR) * MILLISECONDS_PER_HOUR
    }

    /**
     * Retrieves currently running foreground processes.
     */
    private fun getCurrentForegroundProcesses(queryEndTime: Long): Set<String> {
        val foregroundProcesses = mutableSetOf<String>()

        // Only check current processes if the query end time is very recent
        if (queryEndTime >= System.currentTimeMillis() - CURRENT_TIME_THRESHOLD_MILLIS) {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningProcesses = activityManager.runningAppProcesses ?: return foregroundProcesses

            for (process in runningProcesses) {
                if (process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND ||
                    process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
                    foregroundProcesses.add(process.processName)
                }
            }
        }

        return foregroundProcesses
    }

    /**
     * Extracts app foreground sessions from usage events within the specified time range.
     *
     * Processes UsageEvents to track when activities are resumed/paused/stopped, handling:
     * - Activity lifecycle events (RESUMED, PAUSED, STOPPED)
     * - Device shutdown/startup events
     * - Apps still in foreground at query end time
     * - Apps already running at query start time
     */
    private fun extractAppForegroundSessions(
        startTimeMillis: Long,
        endTimeMillis: Long
    ): List<AppForegroundSession> {
        val foregroundProcesses = getCurrentForegroundProcesses(endTimeMillis)
        val foregroundSessions = mutableListOf<AppForegroundSession>()

        // Tracks active components and their start times (null = was active but now stopped)
        val activeComponents = mutableMapOf<AppComponent, Long?>()

        val usageEvents = usageStatsManager.queryEvents(startTimeMillis, endTimeMillis)
        val currentEvent = UsageEvents.Event()

        // Process all usage events in chronological order
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(currentEvent)

            val activityClassName = currentEvent.className ?: continue
            val component = AppComponent(currentEvent.packageName, activityClassName)

            when (currentEvent.eventType) {
                // Activity brought to foreground
                UsageEvents.Event.ACTIVITY_RESUMED,
                EVENT_ACTIVITY_RESUMED_LEGACY -> {
                    activeComponents[component] = currentEvent.timeStamp
                }

                // Activity stopped or paused
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED,
                EVENT_ACTIVITY_STOPPED_LEGACY -> {
                    var sessionStartTime: Long? = activeComponents[component]

                    if (sessionStartTime != null) {
                        // Matched close event - mark component as stopped
                        activeComponents[component] = null
                    } else if (activeComponents.keys.none { it.packageName == currentEvent.packageName }) {
                        // Valid unmatched close - app was already running at query start
                        sessionStartTime = startTimeMillis
                    } else {
                        // Invalid unmatched close - skip it
                        continue
                    }

                    // Calculate session end time (earliest resume of same package or event time)
                    val sessionEndTime = activeComponents.entries
                        .filter { it.key.packageName == currentEvent.packageName && it.value != null }
                        .minOfOrNull { it.value!! }
                        ?: currentEvent.timeStamp

                    foregroundSessions.add(
                        AppForegroundSession(currentEvent.packageName, sessionStartTime, sessionEndTime)
                    )
                }

                // Device shutdown - close all active sessions
                UsageEvents.Event.DEVICE_SHUTDOWN -> {
                    for ((component, startTime) in activeComponents) {
                        if (startTime != null) {
                            foregroundSessions.add(
                                AppForegroundSession(component.packageName, startTime, currentEvent.timeStamp)
                            )
                        }
                    }
                    // Clear all active components
                    activeComponents.keys.forEach { activeComponents[it] = null }
                }

                // Device startup - clear all tracked components
                UsageEvents.Event.DEVICE_STARTUP -> {
                    activeComponents.keys.forEach { activeComponents[it] = null }
                }
            }
        }

        // Handle apps still in foreground at query end time
        for ((component, startTime) in activeComponents) {
            if (startTime == null) continue

            val isStillInForeground = foregroundProcesses.any { it.contains(component.packageName) }
            if (isStillInForeground) {
                val sessionEndTime = minOf(System.currentTimeMillis(), endTimeMillis)
                foregroundSessions.add(
                    AppForegroundSession(component.packageName, startTime, sessionEndTime)
                )
            }
        }

        // Handle edge case: apps in foreground but no events found
        if (activeComponents.isEmpty()) {
            val packageManager = context.packageManager

            for (processName in foregroundProcesses) {
                val isLaunchableApp = packageManager.getLaunchIntentForPackage(processName) != null

                if (isLaunchableApp) {
                    val sessionEndTime = minOf(System.currentTimeMillis(), endTimeMillis)
                    foregroundSessions.add(
                        AppForegroundSession(processName, startTimeMillis, sessionEndTime)
                    )
                }
            }
        }

        return foregroundSessions
    }

    /**
     * Retrieves hourly usage statistics for the specified time range.
     *
     * Returns a map where:
     * - Key: Pair<String, Long> = (packageName, hourStartUtc)
     * - Value: Pair<Long, Int> = (totalUsageTimeMillis, sessionCount)
     *
     * The hourStartUtc represents the start of the hour in UTC milliseconds.
     */
    fun getHourlyUsageStatistics(
        startTimeMillis: Long,
        endTimeMillis: Long
    ): Map<Pair<String, Long>, Pair<Long, Int>> {
        val sessions = extractAppForegroundSessions(startTimeMillis, endTimeMillis)

        // Map: (packageName, hourStartUtc) -> (totalUsageTime, sessionCount)
        val aggregatedData = mutableMapOf<Pair<String, Long>, Pair<Long, Int>>()

        for (session in sessions) {
            if (session.sessionEndTimeMillis <= session.sessionStartTimeMillis) continue

            var currentPosition = session.sessionStartTimeMillis
            val sessionEnd = session.sessionEndTimeMillis

            // Split session across hour boundaries if needed
            while (currentPosition < sessionEnd) {
                val currentHourStart = calculateHourStartMillis(currentPosition)
                val nextHourStart = currentHourStart + MILLISECONDS_PER_HOUR

                val segmentEnd = minOf(sessionEnd, nextHourStart)
                val segmentDuration = segmentEnd - currentPosition

                val key = Pair(session.packageName, currentHourStart)

                val existing = aggregatedData[key]
                aggregatedData[key] = if (existing != null) {
                    Pair(
                        existing.first + segmentDuration,  // totalUsageTimeMillis
                        existing.second + 1                 // sessionCount
                    )
                } else {
                    Pair(segmentDuration, 1)
                }

                currentPosition = segmentEnd
            }
        }

        return aggregatedData
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
}