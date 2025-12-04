package com.librefocus

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class UsageStatsCollector(private val context: Context) {

    companion object {
        private const val TAG = "UsageStatsCollector"
        private const val DEBUG_FOLDER = "debug_raw_data"
    }

    /**
     * Collects usage stats and saves to JSON file
     * @param interval - Use UsageStatsManager.INTERVAL_DAILY, INTERVAL_WEEKLY, etc.
     * @param startTimeUtc - Start time in milliseconds
     * @param endTimeUtc - End time in milliseconds
     */
    suspend fun collectAndStoreUsageStats(
        usageStatsManager: UsageStatsManager,
        interval: Int = UsageStatsManager.INTERVAL_DAILY,
        startTimeUtc: Long,
        endTimeUtc: Long
    ) = withContext(Dispatchers.IO) {
        try {
            // Query usage stats
            val usageStatsList = usageStatsManager.queryUsageStats(
                interval,
                startTimeUtc,
                endTimeUtc
            )

            if (usageStatsList.isNullOrEmpty()) {
                Log.w(TAG, "No usage stats found for the given time range")
                return@withContext
            }

            // Convert to JSON
            val jsonData = convertUsageStatsToJson(
                usageStatsList,
                interval,
                startTimeUtc,
                endTimeUtc
            )

            // Create filename
            val filename = createUsageStatsFilename(startTimeUtc, endTimeUtc)

            // Save to file
            saveToFile(jsonData, filename)

            Log.d(TAG, "Usage stats saved: $filename (${usageStatsList.size} apps)")

        } catch (e: Exception) {
            Log.e(TAG, "Error collecting usage stats", e)
        }
    }

    private fun convertUsageStatsToJson(
        usageStatsList: List<UsageStats>,
        interval: Int,
        startTimeUtc: Long,
        endTimeUtc: Long
    ): String {
        val jsonObject = JSONObject()

        // Metadata
        jsonObject.put("query_start_time_utc", startTimeUtc)
        jsonObject.put("query_end_time_utc", endTimeUtc)
        jsonObject.put("query_start_time_formatted", formatTimestamp(startTimeUtc))
        jsonObject.put("query_end_time_formatted", formatTimestamp(endTimeUtc))
        jsonObject.put("interval", getIntervalName(interval))
        jsonObject.put("total_apps", usageStatsList.size)
        jsonObject.put("collected_at", System.currentTimeMillis())
        jsonObject.put("collected_at_formatted", formatTimestamp(System.currentTimeMillis()))

        // Calculate totals
        var totalScreenTime = 0L
        var totalVisibleTime = 0L
        var totalForegroundServiceTime = 0L

        // Apps array
        val appsArray = JSONArray()

        usageStatsList
            .filter { it.totalTimeInForeground > 0 || it.totalTimeVisible > 0 } // Filter apps with actual usage
            .sortedByDescending { it.totalTimeInForeground } // Sort by screen time
            .forEach { stats ->
                val appJson = JSONObject()

                // Package identification
                appJson.put("package_name", stats.packageName)

                // Time range for this stats object
                appJson.put("first_timestamp_utc", stats.firstTimeStamp)
                appJson.put("first_timestamp_formatted", formatTimestamp(stats.firstTimeStamp))
                appJson.put("last_timestamp_utc", stats.lastTimeStamp)
                appJson.put("last_timestamp_formatted", formatTimestamp(stats.lastTimeStamp))

                // Screen time (foreground interaction)
                appJson.put("total_time_in_foreground_ms", stats.totalTimeInForeground)
                appJson.put("total_time_in_foreground_formatted", formatDuration(stats.totalTimeInForeground))
                appJson.put("total_time_in_foreground_minutes", TimeUnit.MILLISECONDS.toMinutes(stats.totalTimeInForeground))

                // Visible time (app visible but may not be actively used)
                appJson.put("total_time_visible_ms", stats.totalTimeVisible)
                appJson.put("total_time_visible_formatted", formatDuration(stats.totalTimeVisible))
                appJson.put("total_time_visible_minutes", TimeUnit.MILLISECONDS.toMinutes(stats.totalTimeVisible))

                // Foreground service time
                appJson.put("total_time_foreground_service_used_ms", stats.totalTimeForegroundServiceUsed)
                appJson.put("total_time_foreground_service_used_formatted", formatDuration(stats.totalTimeForegroundServiceUsed))

                // Last usage timestamps
                appJson.put("last_time_used_utc", stats.lastTimeUsed)
                appJson.put("last_time_used_formatted", formatTimestamp(stats.lastTimeUsed))
                appJson.put("last_time_visible_utc", stats.lastTimeVisible)
                appJson.put("last_time_visible_formatted", formatTimestamp(stats.lastTimeVisible))
                appJson.put("last_time_foreground_service_used_utc", stats.lastTimeForegroundServiceUsed)
                appJson.put("last_time_foreground_service_used_formatted", formatTimestamp(stats.lastTimeForegroundServiceUsed))

                appsArray.put(appJson)

                // Update totals
                totalScreenTime += stats.totalTimeInForeground
                totalVisibleTime += stats.totalTimeVisible
                totalForegroundServiceTime += stats.totalTimeForegroundServiceUsed
            }

        jsonObject.put("apps", appsArray)

        // Summary statistics
        val summaryJson = JSONObject()
        summaryJson.put("total_screen_time_ms", totalScreenTime)
        summaryJson.put("total_screen_time_formatted", formatDuration(totalScreenTime))
        summaryJson.put("total_screen_time_hours", TimeUnit.MILLISECONDS.toHours(totalScreenTime))
        summaryJson.put("total_visible_time_ms", totalVisibleTime)
        summaryJson.put("total_visible_time_formatted", formatDuration(totalVisibleTime))
        summaryJson.put("total_foreground_service_time_ms", totalForegroundServiceTime)
        summaryJson.put("total_foreground_service_time_formatted", formatDuration(totalForegroundServiceTime))
        summaryJson.put("apps_with_usage", appsArray.length())

        jsonObject.put("summary", summaryJson)

        return jsonObject.toString(4) // Pretty print
    }

    private fun getIntervalName(interval: Int): String {
        return when (interval) {
            UsageStatsManager.INTERVAL_DAILY -> "DAILY"
            UsageStatsManager.INTERVAL_WEEKLY -> "WEEKLY"
            UsageStatsManager.INTERVAL_MONTHLY -> "MONTHLY"
            UsageStatsManager.INTERVAL_YEARLY -> "YEARLY"
            UsageStatsManager.INTERVAL_BEST -> "BEST"
            else -> "UNKNOWN_$interval"
        }
    }

    private fun createUsageStatsFilename(startTimeUtc: Long, endTimeUtc: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
        val startFormatted = dateFormat.format(Date(startTimeUtc))
        val endFormatted = dateFormat.format(Date(endTimeUtc))
        return "usage_stats_${startFormatted}_to_${endFormatted}.json"
    }

    private fun formatTimestamp(timestampUtc: Long): String {
        if (timestampUtc == 0L) return "Never"
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return dateFormat.format(Date(timestampUtc))
    }

    private fun formatDuration(durationMs: Long): String {
        if (durationMs == 0L) return "0s"

        val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60

        return buildString {
            if (hours > 0) append("${hours}h ")
            if (minutes > 0) append("${minutes}m ")
            if (seconds > 0 || (hours == 0L && minutes == 0L)) append("${seconds}s")
        }.trim()
    }

    private fun saveToFile(jsonData: String, filename: String) {
        val debugFolder = File(context.filesDir, DEBUG_FOLDER)

        if (!debugFolder.exists()) {
            debugFolder.mkdirs()
        }

        val file = File(debugFolder, filename)
        file.writeText(jsonData)

        Log.d(TAG, "File saved at: ${file.absolutePath}")
    }

    fun getDebugFolderPath(): String {
        return File(context.filesDir, DEBUG_FOLDER).absolutePath
    }
}
