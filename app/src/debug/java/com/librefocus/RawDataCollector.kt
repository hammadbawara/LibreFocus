package com.librefocus

// Place this file in: src/debug/java/com/yourapp/debug/RawDataCollector.kt

import android.content.Context
import android.util.Log
import com.librefocus.data.local.datasource.UsageStatsDataSource
import com.librefocus.models.UsageEventData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RawDataCollector(
    private val context: Context,
    private val usageStatsDataSource: UsageStatsDataSource
) {

    companion object {
        private const val TAG = "RawDataCollector"
        private const val DEBUG_FOLDER = "debug_raw_data"
    }

    /**
     * Collects raw usage events and stores them in a JSON file
     * File name format: raw_data_YYYY-MM-DD_HH-mm-ss_to_YYYY-MM-DD_HH-mm-ss.json
     */
    suspend fun collectAndStoreRawData(
        startTimeUtc: Long,
        endTimeUtc: Long
    ) = withContext(Dispatchers.IO) {
        try {
            // Fetch raw data using the main function
            val rawEvents = usageStatsDataSource.fetchUsageEvents(startTimeUtc, endTimeUtc)

            // Convert to JSON
            val jsonData = convertToJson(rawEvents, startTimeUtc, endTimeUtc)

            // Create filename with formatted timestamps
            val filename = createFilename(startTimeUtc, endTimeUtc)

            // Save to file
            saveToFile(jsonData, filename)

            Log.d(TAG, "Raw data saved: $filename (${rawEvents.size} events)")

        } catch (e: Exception) {
            Log.e(TAG, "Error collecting and storing raw data", e)
        }
    }

    private fun convertToJson(
        events: List<UsageEventData>,
        startTimeUtc: Long,
        endTimeUtc: Long
    ): String {
        val jsonObject = JSONObject()

        // Metadata
        jsonObject.put("start_time_utc", startTimeUtc)
        jsonObject.put("end_time_utc", endTimeUtc)
        jsonObject.put("start_time_formatted", formatTimestamp(startTimeUtc))
        jsonObject.put("end_time_formatted", formatTimestamp(endTimeUtc))
        jsonObject.put("total_events", events.size)
        jsonObject.put("collected_at", System.currentTimeMillis())

        // Events array
        val eventsArray = JSONArray()
        events.forEach { event ->
            val eventJson = JSONObject()
            eventJson.put("package_name", event.packageName)
            eventJson.put("timestamp_utc", event.timestampUtc)
            eventJson.put("timestamp_formatted", formatTimestamp(event.timestampUtc))
            eventJson.put("event_type", event.eventType)
            eventJson.put("event_type_name", getEventTypeName(event.eventType))
            eventsArray.put(eventJson)
        }
        jsonObject.put("events", eventsArray)

        return jsonObject.toString(4) // Pretty print with 4 space indent
    }

    private fun createFilename(startTimeUtc: Long, endTimeUtc: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
        val startFormatted = dateFormat.format(Date(startTimeUtc))
        val endFormatted = dateFormat.format(Date(endTimeUtc))
        return "raw_data_${startFormatted}_to_${endFormatted}.json"
    }

    private fun formatTimestamp(timestampUtc: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return dateFormat.format(Date(timestampUtc))
    }

    private fun getEventTypeName(eventType: Int): String {
        return when (eventType) {
            1 -> "ACTIVITY_RESUMED"
            2 -> "ACTIVITY_PAUSED"
            23 -> "ACTIVITY_STOPPED"
            else -> "UNKNOWN_$eventType"
        }
    }

    private fun saveToFile(jsonData: String, filename: String) {
        // Get the debug folder in app's internal storage
        val debugFolder = File(context.filesDir, DEBUG_FOLDER)

        // Create folder if it doesn't exist
        if (!debugFolder.exists()) {
            debugFolder.mkdirs()
        }

        // Write to file
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
     * List all saved raw data files
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

