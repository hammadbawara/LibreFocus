package com.librefocus.data.repository

import com.librefocus.data.local.database.dao.CompleteLimitData
import com.librefocus.data.local.database.dao.LimitDao
import com.librefocus.data.local.database.entity.LimitEntity
import com.librefocus.data.local.database.mapper.toEntities
import com.librefocus.models.DayOfWeek
import com.librefocus.models.Limit
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LimitRepositoryTest {

    private lateinit var limitDao: LimitDao
    private lateinit var repository: LimitRepository

    @Before
    fun setup() {
        limitDao = mockk()
        repository = LimitRepository(limitDao)
    }

    private fun createTestLimit(): Limit {
        return Limit.Schedule(
            id = "test-id",
            name = "Test Limit",
            isEnabled = true,
            selectedAppPackages = emptyList(),
            createdAt = 100L,
            updatedAt = 200L,
            isAllDay = true,
            timeSlots = emptyList(),
            selectedDays = setOf(DayOfWeek.MON)
        )
    }

    @Test
    fun `isLimitExists returns properly`() = runTest {
        coEvery { limitDao.isLimitExists("test-id") } returns true
        assertTrue(repository.isLimitExists("test-id"))

        coEvery { limitDao.isLimitExists("missing-id") } returns false
        assertFalse(repository.isLimitExists("missing-id"))
    }

    @Test
    fun `getLimitById returns mapped domain model`() = runTest {
        val limit = createTestLimit()
        val entities = limit.toEntities()
        val completeLimit = CompleteLimitData(
            limit = entities.limit,
            scheduleLimit = entities.scheduleLimit,
            usageLimit = entities.usageLimit,
            launchCountLimit = entities.launchCountLimit
        )

        coEvery { limitDao.getCompleteLimitById("test-id") } returns completeLimit

        val result = repository.getLimitById("test-id")
        assertEquals("test-id", result?.id)
        assertEquals("Test Limit", result?.name)
    }

    @Test
    fun `insertLimit inserts complete limit via dao and returns success`() = runTest {
        val limit = createTestLimit()
        coEvery { limitDao.insertCompleteLimit(any(), any(), any(), any()) } returns Unit

        val result = repository.insertLimit(limit)

        assertTrue(result.isSuccess)
        assertEquals(limit, result.getOrNull())
        
        coVerify { 
            limitDao.insertCompleteLimit(
                match { it.id == "test-id" },
                any(), any(), any()
            ) 
        }
    }

    @Test
    fun `updateLimit updates timestamp and calls dao`() = runTest {
        val limit = createTestLimit() // updatedAt = 200L
        coEvery { limitDao.updateCompleteLimit(any(), any(), any(), any()) } returns Unit

        val result = repository.updateLimit(limit)

        assertTrue(result.isSuccess)
        val updatedLimit = result.getOrNull()!!
        assertTrue(updatedLimit.updatedAt > 200L) // Should be updated
        
        coVerify { 
            limitDao.updateCompleteLimit(
                match { it.id == "test-id" && it.updatedAt > 200L },
                any(), any(), any()
            ) 
        }
    }

    @Test
    fun `toggleLimitEnabled updates limit enabled status`() = runTest {
        coEvery { limitDao.updateLimitEnabledStatus("test-id", false) } returns Unit

        val result = repository.toggleLimitEnabled("test-id", false)

        assertTrue(result.isSuccess)
        coVerify { limitDao.updateLimitEnabledStatus("test-id", false) }
    }

    @Test
    fun `deleteLimit removes by id successfully`() = runTest {
        coEvery { limitDao.deleteCompleteLimit("test-id") } returns Unit

        val result = repository.deleteLimit("test-id")

        assertTrue(result.isSuccess)
        coVerify { limitDao.deleteCompleteLimit("test-id") }
    }

    @Test
    fun `deleteLimits removes multiple ids and returns count`() = runTest {
        coEvery { limitDao.deleteCompleteLimit("id1") } returns Unit
        coEvery { limitDao.deleteCompleteLimit("id2") } returns Unit

        val result = repository.deleteLimits(listOf("id1", "id2"))

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())
    }

    @Test
    fun `enableAllLimits toggles all existing limits`() = runTest {
        val limitEntity1 = LimitEntity(id = "id1", name = "1", isEnabled = false, limitType = "SCHEDULE", selectedAppPackages = "", createdAt = 0L, updatedAt = 0L)
        val limitEntity2 = LimitEntity(id = "id2", name = "2", isEnabled = false, limitType = "USAGE", selectedAppPackages = "", createdAt = 0L, updatedAt = 0L)
        
        coEvery { limitDao.getAllLimitsAsList() } returns listOf(limitEntity1, limitEntity2)
        coEvery { limitDao.updateLimitEnabledStatus(any(), true) } returns Unit

        val result = repository.enableAllLimits()

        assertTrue(result.isSuccess)
        coVerify { limitDao.updateLimitEnabledStatus("id1", true) }
        coVerify { limitDao.updateLimitEnabledStatus("id2", true) }
    }
}
