@file:JvmName("DebugActivityKt")

package com.librefocus

import android.app.usage.UsageStatsManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.librefocus.data.local.datasource.UsageStatsDataSource
import kotlinx.coroutines.launch

class DebugActivity : ComponentActivity() {
    private lateinit var rawDataCollector: RawDataCollector
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val dataSource = UsageStatsDataSource(this, usageStatsManager)
        rawDataCollector = RawDataCollector(this, dataSource)

        // Collect data for last 24 hours
        lifecycleScope.launch {
//            val endTime = System.currentTimeMillis()
//            val startTime = endTime - (24 * 60 * 60 * 1000) // 24 hours ago

            //rawDataCollector.collectAndStoreRawData(1764230400000, 1764261454345)

//            UsageStatsCollector(this@DebugActivity).collectAndStoreUsageStats(
//                usageStatsManager = usageStatsManager,
//                startTimeUtc = 1764230400000,
//                endTimeUtc = 1764234000000
//            )

//            // Log where files are saved
//            Log.d("DEBUG", "Files saved in: ${rawDataCollector.getDebugFolderPath()}")
//            Log.d("DEBUG", "Saved files: ${rawDataCollector.listSavedFiles()}")

            val usageAggregateStatsCollector = UsageAggregateStatsCollector(this@DebugActivity, usageStatsManager)

            usageAggregateStatsCollector.collectAndStoreUsageStats(
                startTimeUtc = 1764230400000,
                endTimeUtc = 1764234000000
            )
        }
    }
}