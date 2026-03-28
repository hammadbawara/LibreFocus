package com.librefocus.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.librefocus.data.local.database.entity.GamificationStatsEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for gamification statistics.
 */
@Dao
interface GamificationStatsDao {
    
    @Query("SELECT * FROM gamification_stats WHERE id = 1 LIMIT 1")
    fun getStats(): Flow<GamificationStatsEntity?>
    
    @Query("SELECT * FROM gamification_stats WHERE id = 1 LIMIT 1")
    suspend fun getStatsOnce(): GamificationStatsEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: GamificationStatsEntity)
    
    @Update
    suspend fun update(stats: GamificationStatsEntity)
    
    @Query("DELETE FROM gamification_stats")
    suspend fun clear()
}

