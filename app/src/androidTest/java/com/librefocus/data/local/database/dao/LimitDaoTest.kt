package com.librefocus.data.local.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.librefocus.data.local.database.UsageDatabase
import com.librefocus.data.local.database.entity.LimitEntity
import com.librefocus.data.local.database.entity.ScheduleLimitEntity
import com.librefocus.data.local.database.entity.UsageLimitEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LimitDaoTest {

    private lateinit var db: UsageDatabase
    private lateinit var limitDao: LimitDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, UsageDatabase::class.java
        ).build()
        limitDao = db.limitDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetLimit() = runBlocking {
        val limitId = "test_limit_1"
        val limit = LimitEntity(
            id = limitId,
            name = "Test Limit",
            isEnabled = true,
            limitType = "SCHEDULE",
            selectedAppPackages = "[\"com.test.app\"]",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        limitDao.insertLimit(limit)
        
        val retrieved = limitDao.getLimitById(limitId)
        
        assertNotNull(retrieved)
        assertEquals("Test Limit", retrieved?.name)
        assertEquals("SCHEDULE", retrieved?.limitType)
    }

    @Test
    fun insertCompleteScheduleLimit() = runBlocking {
        val limitId = "test_limit_schedule"
        val limit = LimitEntity(
            id = limitId,
            name = "Work Hours",
            isEnabled = true,
            limitType = "SCHEDULE",
            selectedAppPackages = "[\"com.social.app\"]",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        val scheduleLimit = ScheduleLimitEntity(
            limitId = limitId,
            isAllDay = false,
            timeSlots = "[{\"startMinutes\": 540, \"endMinutes\": 1020}]",
            selectedDays = "MON,TUE,WED,THU,FRI"
        )
        
        limitDao.insertCompleteLimit(
            limit = limit,
            scheduleLimit = scheduleLimit
        )
        
        val completeData = limitDao.getCompleteLimitById(limitId)
        
        assertNotNull(completeData)
        assertEquals("Work Hours", completeData?.limit?.name)
        assertNotNull(completeData?.scheduleLimit)
        assertEquals("MON,TUE,WED,THU,FRI", completeData?.scheduleLimit?.selectedDays)
        assertNull(completeData?.usageLimit)
    }

    @Test
    fun deleteCompleteLimitCascades() = runBlocking {
        val limitId = "test_limit_usage"
        val limit = LimitEntity(
            id = limitId,
            name = "Daily Social",
            isEnabled = true,
            limitType = "USAGE_LIMIT",
            selectedAppPackages = "[\"com.social.app\"]",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        val usageLimit = UsageLimitEntity(
            limitId = limitId,
            limitType = "DAILY",
            durationMinutes = 60,
            selectedDays = "MON,TUE,WED,THU,FRI,SAT,SUN"
        )
        
        limitDao.insertCompleteLimit(
            limit = limit,
            usageLimit = usageLimit
        )
        
        // Verify insertion
        var completeData = limitDao.getCompleteLimitById(limitId)
        assertNotNull(completeData)
        assertNotNull(completeData?.usageLimit)
        
        // Delete limit
        limitDao.deleteCompleteLimit(limitId)
        
        // Verify deletion
        completeData = limitDao.getCompleteLimitById(limitId)
        assertNull(completeData) // Should be null entirely
        
        // Verify cascading deletion of usage limit
        val retrievedUsage = limitDao.getUsageLimitByLimitId(limitId)
        assertNull(retrievedUsage)
    }

    @Test
    fun getAllLimitsOrderAndFiltering() = runBlocking {
        limitDao.insertLimit(LimitEntity("id1", "Limit 1", true, "SCHEDULE", "[]", 1000L, 1000L))
        limitDao.insertLimit(LimitEntity("id2", "Limit 2", false, "USAGE_LIMIT", "[]", 2000L, 2000L))
        limitDao.insertLimit(LimitEntity("id3", "Limit 3", true, "LAUNCH_COUNT", "[]", 3000L, 3000L))
        
        val allLimits = limitDao.getAllLimits().first()
        assertEquals(3, allLimits.size)
        // Ordered by createdAt DESC
        assertEquals("Limit 3", allLimits[0].name)
        assertEquals("Limit 2", allLimits[1].name)
        assertEquals("Limit 1", allLimits[2].name)
        
        val enabledLimits = limitDao.getEnabledLimits().first()
        assertEquals(2, enabledLimits.size)
        assertTrue(enabledLimits.all { it.isEnabled })
    }
}
