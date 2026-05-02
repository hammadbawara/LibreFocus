package com.librefocus.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.librefocus.data.local.database.entity.AchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {

    @Query("SELECT * FROM achievements ORDER BY achievedAtUtc DESC")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements ORDER BY achievedAtUtc DESC")
    fun getAllAchievementsSync(): List<AchievementEntity>

    @Query("SELECT * FROM achievements WHERE type = :type ORDER BY achievedAtUtc DESC")
    suspend fun getAchievementsByType(type: String): List<AchievementEntity>

    @Query("SELECT COUNT(*) FROM achievements WHERE type = :type")
    suspend fun countAchievementsByType(type: String): Int

    @Query("SELECT * FROM achievements WHERE type = :type AND sourceDateUtc = :sourceDateUtc LIMIT 1")
    suspend fun getAchievementByTypeAndSourceDate(type: String, sourceDateUtc: Long): AchievementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(entity: AchievementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAchievementsSync(entities: List<AchievementEntity>): List<Long>

    @Query("DELETE FROM achievements")
    fun deleteAllAchievements()
}