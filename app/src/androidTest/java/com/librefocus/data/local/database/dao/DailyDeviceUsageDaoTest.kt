package com.librefocus.data.local.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.librefocus.data.local.database.UsageDatabase
import com.librefocus.data.local.database.entity.DailyDeviceUsageEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class DailyDeviceUsageDaoTest {

    private lateinit var db: UsageDatabase
    private lateinit var dailyDeviceUsageDao: DailyDeviceUsageDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, UsageDatabase::class.java
        ).build()
        dailyDeviceUsageDao = db.dailyDeviceUsageDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetDailyUsage() = runBlocking {
        val dateUtc = System.currentTimeMillis()
        val hourlyUnlocks = mapOf(0 to 2, 8 to 5, 12 to 10)
        
        val usage = DailyDeviceUsageEntity(
            dateUtc = dateUtc,
            hourlyUnlocks = hourlyUnlocks,
            totalUnlocks = 17,
            lastUpdatedUtc = System.currentTimeMillis()
        )
        
        dailyDeviceUsageDao.insertOrUpdateDailyUsage(usage)
        
        val retrievedUsage = dailyDeviceUsageDao.getUsageForDate(dateUtc)
        
        assertNotNull(retrievedUsage)
        assertEquals(17, retrievedUsage?.totalUnlocks)
        assertEquals(3, retrievedUsage?.hourlyUnlocks?.size)
        assertEquals(5, retrievedUsage?.hourlyUnlocks?.get(8))
    }

    @Test
    fun getUsageForDayRange() = runBlocking {
        val baseDateUtc = 1600000000000L // arbitrary date
        val dayInMs = TimeUnit.DAYS.toMillis(1)
        
        // Insert usage for 3 consecutive days
        for (i in 0..2) {
            val usage = DailyDeviceUsageEntity(
                dateUtc = baseDateUtc + (i * dayInMs),
                hourlyUnlocks = mapOf(1 to i),
                totalUnlocks = i,
                lastUpdatedUtc = System.currentTimeMillis()
            )
            dailyDeviceUsageDao.insertOrUpdateDailyUsage(usage)
        }
        
        // Insert a usage outside the range
        val usageOutside = DailyDeviceUsageEntity(
            dateUtc = baseDateUtc + (10 * dayInMs),
            hourlyUnlocks = emptyMap(),
            totalUnlocks = 0,
            lastUpdatedUtc = System.currentTimeMillis()
        )
        dailyDeviceUsageDao.insertOrUpdateDailyUsage(usageOutside)
        
        // Query for the first 2 days
        val result = dailyDeviceUsageDao.getUsageForDayRange(
            startUtc = baseDateUtc,
            endUtc = baseDateUtc + (2 * dayInMs)
        )
        
        // Should return data for day 0 and day 1
        assertEquals(2, result.size)
        assertEquals(baseDateUtc, result[0].dateUtc)
        assertEquals(baseDateUtc + dayInMs, result[1].dateUtc)
    }

    @Test
    fun getNonExistentUsageReturnsNull() = runBlocking {
        val retrieved = dailyDeviceUsageDao.getUsageForDate(123456789L)
        assertNull(retrieved)
    }
}
