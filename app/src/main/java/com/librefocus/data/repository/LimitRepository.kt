package com.librefocus.data.repository

import com.librefocus.data.local.database.dao.LimitDao
import com.librefocus.data.local.database.mapper.toDomainModel
import com.librefocus.data.local.database.mapper.toEntities
import com.librefocus.data.local.database.mapper.withUpdatedTimestamp
import com.librefocus.models.Limit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LimitRepository(private val limitDao: LimitDao) {

    fun getAllLimits(): Flow<List<Limit>> = limitDao.getAllLimits().map { limitEntities ->
        withContext(Dispatchers.IO) {
            val completeLimits = limitDao.getAllCompleteLimits()

            completeLimits.mapNotNull { it.toDomainModel() }
        }
    }

    fun getLimitCount(): Flow<Int> = limitDao.getLimitCount()


    fun getEnabledLimits(): Flow<List<Limit>> = limitDao.getEnabledLimits().map { limitEntities ->
        withContext(Dispatchers.IO) {
            // Fetch complete data for enabled limits
            limitEntities.mapNotNull { entity ->
                limitDao.getCompleteLimitById(entity.id)?.toDomainModel()
            }
        }
    }

    /**
     * Gets a single limit by ID.
     * Returns null if the limit doesn't exist or data is inconsistent.
     */
    suspend fun getLimitById(id: String): Limit? = withContext(Dispatchers.IO) {
        limitDao.getCompleteLimitById(id)?.toDomainModel()
    }
    
    /**
     * Checks if a limit with the given ID exists.
     */
    suspend fun isLimitExists(id: String): Boolean = withContext(Dispatchers.IO) {
        limitDao.isLimitExists(id)
    }
    
    // ========== Create Operations ==========
    
    /**
     * Inserts a new limit into the database.
     * The limit's ID, createdAt, and updatedAt should already be set.
     * 
     * @return The inserted limit, or null if insertion failed
     */
    suspend fun insertLimit(limit: Limit): Result<Limit> = withContext(Dispatchers.IO) {
        try {
            val entities = limit.toEntities()
            limitDao.insertCompleteLimit(
                limit = entities.limit,
                scheduleLimit = entities.scheduleLimit,
                usageLimit = entities.usageLimit,
                launchCountLimit = entities.launchCountLimit
            )
            Result.success(limit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ========== Update Operations ==========
    
    /**
     * Updates an existing limit.
     * Automatically updates the updatedAt timestamp.
     * 
     * @return The updated limit, or null if update failed
     */
    suspend fun updateLimit(limit: Limit): Result<Limit> = withContext(Dispatchers.IO) {
        try {
            val updatedLimit = limit.withUpdatedTimestamp()
            val entities = updatedLimit.toEntities()
            limitDao.updateCompleteLimit(
                limit = entities.limit,
                scheduleLimit = entities.scheduleLimit,
                usageLimit = entities.usageLimit,
                launchCountLimit = entities.launchCountLimit
            )
            Result.success(updatedLimit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Toggles the enabled status of a limit.
     * More efficient than updating the entire limit when only changing enabled state.
     */
    suspend fun toggleLimitEnabled(id: String, isEnabled: Boolean): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                limitDao.updateLimitEnabledStatus(id, isEnabled)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    // ========== Delete Operations ==========
    
    /**
     * Deletes a limit and all its related data.
     * Foreign key constraints with CASCADE ensure related data is deleted automatically.
     */
    suspend fun deleteLimit(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            limitDao.deleteCompleteLimit(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Deletes a limit by passing the limit object.
     */
    suspend fun deleteLimit(limit: Limit): Result<Unit> = deleteLimit(limit.id)
    
    /**
     * Deletes multiple limits by their IDs.
     */
    suspend fun deleteLimits(ids: List<String>): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var deletedCount = 0
            ids.forEach { id ->
                limitDao.deleteCompleteLimit(id)
                deletedCount++
            }
            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ========== Batch Operations ==========
    
    /**
     * Inserts multiple limits in a batch operation.
     * Returns the number of successfully inserted limits.
     */
    suspend fun insertLimits(limits: List<Limit>): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var insertedCount = 0
            limits.forEach { limit ->
                val entities = limit.toEntities()
                limitDao.insertCompleteLimit(
                    limit = entities.limit,
                    scheduleLimit = entities.scheduleLimit,
                    usageLimit = entities.usageLimit,
                    launchCountLimit = entities.launchCountLimit
                )
                insertedCount++
            }
            Result.success(insertedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Enables all limits.
     */
    suspend fun enableAllLimits(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val allLimits = limitDao.getAllLimitsAsList()
            allLimits.forEach { limit ->
                limitDao.updateLimitEnabledStatus(limit.id, true)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Disables all limits.
     */
    suspend fun disableAllLimits(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val allLimits = limitDao.getAllLimitsAsList()
            allLimits.forEach { limit ->
                limitDao.updateLimitEnabledStatus(limit.id, false)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
