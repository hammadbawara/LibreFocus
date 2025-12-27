package com.librefocus.ui.chatbot

import com.librefocus.data.local.database.dao.AppDao
import com.librefocus.data.local.database.dao.DailyDeviceUsageDao
import com.librefocus.data.local.database.dao.HourlyAppUsageDao
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

interface IChatContextProvider {
    suspend fun getBehaviorContext(): String
}

class ChatContextProvider(
    private val dailyDeviceUsageDao: DailyDeviceUsageDao,
    private val hourlyAppUsageDao: HourlyAppUsageDao,
    private val appDao: AppDao
) : IChatContextProvider {

    override suspend fun getBehaviorContext(): String {
        val today = LocalDate.now()
        val startOfToday = today.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000
        val endOfToday = today.plusDays(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000

        val totalScreenTimeMillis = hourlyAppUsageDao.getTotalUsageInTimeRange(startOfToday, endOfToday) ?: 0L
        val totalScreenTime = TimeUnit.MILLISECONDS.toMinutes(totalScreenTimeMillis).toInt()

        val hourlyUsage = hourlyAppUsageDao.getUsageInTimeRangeOnce(startOfToday, endOfToday)
        val topApps = hourlyUsage
            .groupBy { it.appId }
            .mapValues { (_, usages) -> usages.sumOf { it.usageDurationMillis } }
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .mapNotNull { appDao.getAppById(it.key)?.appName }
            .joinToString(", ")

        return """
        USER'S CURRENT BEHAVIOR CONTEXT:
        - Daily screen time: $totalScreenTime minutes
        - Top apps: $topApps
        """
    }
}
