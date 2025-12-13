@file:JvmName("DebugActivityKt")

package com.librefocus

import android.app.usage.UsageStatsManager
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
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

        val packageManager = packageManager

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

//            val usageAggregateStatsCollector = UsageAggregateStatsCollector(this@DebugActivity, usageStatsManager)
//
//            usageAggregateStatsCollector.collectAndStoreUsageStats(
//                startTimeUtc = 1764230400000,
//                endTimeUtc = 1764234000000
//            )

            val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

            for (app in apps) {
                val category = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ApplicationInfo.getCategoryTitle(this@DebugActivity, app.category)
                } else {
                    "Unknown (below API 26)"
                }

                val label = packageManager.getApplicationLabel(app).toString()
                val pkg = app.packageName

                println("$pkg -> $label -> $category")
            }


        }
    }
}