package com.librefocus.ai

import com.librefocus.data.local.database.dao.DailyDeviceUsageDao
import com.librefocus.data.local.database.dao.HourlyAppUsageDao
import com.librefocus.data.local.database.dao.AppDao
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeHourlyDao : HourlyAppUsageDao {
    override fun getUsageByApp(appId: Int) = throw NotImplementedError()
    override fun getUsageInTimeRange(startUtc: Long, endUtc: Long) = throw NotImplementedError()
    override fun getUsageInTimeRangeOnce(startUtc: Long, endUtc: Long) = emptyList()
    override fun getUsageForAppAtHour(appId: Int, hourStartUtc: Long) = throw NotImplementedError()
    override suspend fun insertUsage(usage: com.librefocus.data.local.database.entity.HourlyAppUsageEntity) = throw NotImplementedError()
    override suspend fun insertUsages(usages: List<com.librefocus.data.local.database.entity.HourlyAppUsageEntity>) = throw NotImplementedError()
    override suspend fun updateUsage(usage: com.librefocus.data.local.database.entity.HourlyAppUsageEntity) = throw NotImplementedError()
    override suspend fun deleteUsage(usage: com.librefocus.data.local.database.entity.HourlyAppUsageEntity) = throw NotImplementedError()
    override fun getAllUsage() = throw NotImplementedError()
    override fun getAllUsageSync() = throw NotImplementedError()
    override fun insertUsageSync(usages: List<com.librefocus.data.local.database.entity.HourlyAppUsageEntity>) = throw NotImplementedError()
}

class FakeDailyDao : DailyDeviceUsageDao {
    override fun getAllDailyUsage() = throw NotImplementedError()
    override suspend fun getDailyUsageForDay(dayStartUtc: Long) = throw NotImplementedError()
}

class FakeAppDao : AppDao {
    override suspend fun getAppByPackageName(packageName: String) = null
    override suspend fun getAppById(id: Int) = null
    override suspend fun insertApp(app: com.librefocus.data.local.database.entity.AppEntity) = throw NotImplementedError()
}

class ChatContextProviderTest {

    @Test
    fun buildSummary_noData_returnsNoDataTrue() = runBlocking {
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val provider = ChatContextProvider(FakeDailyDao(), FakeHourlyDao(), FakeAppDao(), ctx)
        val now = System.currentTimeMillis()
        val summary = provider.buildSummary(now - 24*60*60*1000, now, rawUserId = "user-1", goalMinutes = 60)
        assertTrue(summary.noData)
    }
}

