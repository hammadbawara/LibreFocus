package com.librefocus

// Place this file in: src/debug/java/com/yourapp/debug/UsageStatsCollector.kt

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

class UsageAggregateStatsCollector(
    private val context: Context,
    private val usageStatsManager: UsageStatsManager
) {

    companion object {
        private const val TAG = "UsageAggregateStatsCollector"
        private const val DEBUG_FOLDER = "debug_usage_stats"
    }

    /**
     * Collects aggregated usage stats and stores them in a JSON file
     * File name format: usage_stats_YYYY-MM-DD_HH-mm-ss_to_YYYY-MM-DD_HH-mm-ss.json
     */
    suspend fun collectAndStoreUsageStats(
        startTimeUtc: Long,
        endTimeUtc: Long
    ) = withContext(Dispatchers.IO) {
        try {
            // Fetch aggregated usage stats
            val usageStatsMap = usageStatsManager.queryAndAggregateUsageStats(
                startTimeUtc,
                endTimeUtc
            )

            if (usageStatsMap.isNullOrEmpty()) {
                Log.w(TAG, "No usage stats available for the time range")
                return@withContext
            }

            // Convert to JSON
            val jsonData = convertToJson(usageStatsMap, startTimeUtc, endTimeUtc)

            // Create filename with formatted timestamps
            val filename = createFilename(startTimeUtc, endTimeUtc)

            // Save to file
            saveToFile(jsonData, filename)

            Log.d(TAG, "Usage stats saved: $filename (${usageStatsMap.size} apps)")

        } catch (e: Exception) {
            Log.e(TAG, "Error collecting and storing usage stats", e)
        }
    }

    private fun convertToJson(
        usageStatsMap: Map<String, UsageStats>,
        startTimeUtc: Long,
        endTimeUtc: Long
    ): String {
        val jsonObject = JSONObject()

        // Metadata
        val durationMillis = endTimeUtc - startTimeUtc
        jsonObject.put("start_time_utc", startTimeUtc)
        jsonObject.put("end_time_utc", endTimeUtc)
        jsonObject.put("start_time_formatted", formatTimestamp(startTimeUtc))
        jsonObject.put("end_time_formatted", formatTimestamp(endTimeUtc))
        jsonObject.put("duration_millis", durationMillis)
        jsonObject.put("duration_formatted", formatDuration(durationMillis))
        jsonObject.put("total_apps", usageStatsMap.size)
        jsonObject.put("collected_at", System.currentTimeMillis())

        // Calculate total screen time across all apps
        var totalScreenTime = 0L
        usageStatsMap.values.forEach { stats ->
            totalScreenTime += stats.totalTimeInForeground
        }
        jsonObject.put("total_screen_time_millis", totalScreenTime)
        jsonObject.put("total_screen_time_formatted", formatDuration(totalScreenTime))

        // Apps array - sorted by screen time (descending)
        val appsArray = JSONArray()
        val sortedStats = usageStatsMap.values.sortedByDescending { it.totalTimeInForeground }

        sortedStats.forEach { stats ->
            val appJson = JSONObject()

            // Basic info
            appJson.put("package_name", stats.packageName)

            // Time stats
            appJson.put("first_time_stamp", stats.firstTimeStamp)
            appJson.put("first_time_stamp_formatted", formatTimestamp(stats.firstTimeStamp))
            appJson.put("last_time_stamp", stats.lastTimeStamp)
            appJson.put("last_time_stamp_formatted", formatTimestamp(stats.lastTimeStamp))
            appJson.put("last_time_used", stats.lastTimeUsed)
            appJson.put("last_time_used_formatted", formatTimestamp(stats.lastTimeUsed))

            // Foreground time (screen time)
            val foregroundTime = stats.totalTimeInForeground
            appJson.put("total_time_in_foreground_millis", foregroundTime)
            appJson.put("total_time_in_foreground_formatted", formatDuration(foregroundTime))
            appJson.put("total_time_in_foreground_minutes", TimeUnit.MILLISECONDS.toMinutes(foregroundTime))
            appJson.put("total_time_in_foreground_seconds", TimeUnit.MILLISECONDS.toSeconds(foregroundTime))

            // Percentage of total screen time
            val percentage = if (totalScreenTime > 0) {
                (foregroundTime.toDouble() / totalScreenTime.toDouble()) * 100
            } else {
                0.0
            }
            appJson.put("screen_time_percentage", String.format("%.2f", percentage))

            // Additional useful data
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Available on API 29+
                appJson.put("last_time_foreground_service_used", stats.lastTimeForegroundServiceUsed)
                appJson.put("last_time_foreground_service_used_formatted",
                    formatTimestamp(stats.lastTimeForegroundServiceUsed))
                appJson.put("total_time_foreground_service_used_millis",
                    stats.totalTimeForegroundServiceUsed)
                appJson.put("total_time_foreground_service_used_formatted",
                    formatDuration(stats.totalTimeForegroundServiceUsed))
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                // Available on API 31+
                appJson.put("last_time_visible", stats.lastTimeVisible)
                appJson.put("last_time_visible_formatted", formatTimestamp(stats.lastTimeVisible))
                appJson.put("total_time_visible_millis", stats.totalTimeVisible)
                appJson.put("total_time_visible_formatted", formatDuration(stats.totalTimeVisible))
            }

            appsArray.put(appJson)
        }

        jsonObject.put("apps", appsArray)

        // Summary statistics
        val summaryJson = JSONObject()
        summaryJson.put("apps_with_usage", sortedStats.count { it.totalTimeInForeground > 0 })
        summaryJson.put("apps_without_usage", sortedStats.count { it.totalTimeInForeground == 0L })

        if (sortedStats.isNotEmpty()) {
            val topApp = sortedStats.first()
            summaryJson.put("most_used_app", topApp.packageName)
            summaryJson.put("most_used_app_time_millis", topApp.totalTimeInForeground)
            summaryJson.put("most_used_app_time_formatted", formatDuration(topApp.totalTimeInForeground))
        }

        jsonObject.put("summary", summaryJson)

        return jsonObject.toString(4) // Pretty print with 4 space indent
    }

    private fun createFilename(startTimeUtc: Long, endTimeUtc: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
        val startFormatted = dateFormat.format(Date(startTimeUtc))
        val endFormatted = dateFormat.format(Date(endTimeUtc))
        return "usage_aggregate_stats_${startFormatted}_to_${endFormatted}.json"
    }

    private fun formatTimestamp(timestampUtc: Long): String {
        if (timestampUtc == 0L) return "N/A"
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return dateFormat.format(Date(timestampUtc))
    }

    private fun formatDuration(durationMillis: Long): String {
        if (durationMillis == 0L) return "0s"

        val days = TimeUnit.MILLISECONDS.toDays(durationMillis)
        val hours = TimeUnit.MILLISECONDS.toHours(durationMillis) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60

        val parts = mutableListOf<String>()
        if (days > 0) parts.add("${days}d")
        if (hours > 0) parts.add("${hours}h")
        if (minutes > 0) parts.add("${minutes}m")
        if (seconds > 0 || parts.isEmpty()) parts.add("${seconds}s")

        return parts.joinToString(" ")
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

    /**
     * Get the debug folder path for reference
     */
    fun getDebugFolderPath(): String {
        return File(context.filesDir, DEBUG_FOLDER).absolutePath
    }

    /**
     * List all saved usage stats files
     */
    fun listSavedFiles(): List<String> {
        val debugFolder = File(context.filesDir, DEBUG_FOLDER)
        return debugFolder.listFiles()?.map { it.name }?.sorted() ?: emptyList()
    }

    /**
     * Delete old files (optional cleanup)
     */
    fun deleteOldFiles(daysToKeep: Int = 7) {
        val debugFolder = File(context.filesDir, DEBUG_FOLDER)
        val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)

        debugFolder.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoffTime) {
                file.delete()
                Log.d(TAG, "Deleted old file: ${file.name}")
            }
        }
    }
}

// Example usage in your debug code:
/*
class DebugActivity : AppCompatActivity() {

    private lateinit var usageStatsCollector: UsageStatsCollector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            usageStatsCollector = UsageStatsCollector(this, usageStatsManager)

            // Collect data for last 24 hours
            lifecycleScope.launch {
                val endTime = System.currentTimeMillis()
                val startTime = endTime - (24 * 60 * 60 * 1000) // 24 hours ago

                usageStatsCollector.collectAndStoreUsageStats(startTime, endTime)

                // Log where files are saved
                Log.d("DEBUG", "Files saved in: ${usageStatsCollector.getDebugFolderPath()}")
                Log.d("DEBUG", "Saved files: ${usageStatsCollector.listSavedFiles()}")
            }
        }
    }
}
*/