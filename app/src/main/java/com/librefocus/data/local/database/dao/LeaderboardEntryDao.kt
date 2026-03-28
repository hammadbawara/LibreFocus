package com.librefocus.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.librefocus.data.local.database.entity.LeaderboardEntryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for leaderboard entries.
 */
@Dao
interface LeaderboardEntryDao {
    
    @Query("SELECT * FROM leaderboard_entries ORDER BY points DESC, rank ASC")
    fun getLeaderboard(): Flow<List<LeaderboardEntryEntity>>
    
    @Query("SELECT * FROM leaderboard_entries ORDER BY points DESC, rank ASC")
    suspend fun getLeaderboardOnce(): List<LeaderboardEntryEntity>
    
    @Query("SELECT * FROM leaderboard_entries WHERE userId = :userId LIMIT 1")
    suspend fun getEntryByUserId(userId: String): LeaderboardEntryEntity?
    
    @Query("SELECT * FROM leaderboard_entries ORDER BY points DESC LIMIT :limit")
    suspend fun getTopEntries(limit: Int = 10): List<LeaderboardEntryEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: LeaderboardEntryEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<LeaderboardEntryEntity>)
    
    @Update
    suspend fun updateEntry(entry: LeaderboardEntryEntity)
    
    @Delete
    suspend fun deleteEntry(entry: LeaderboardEntryEntity)
    
    @Query("DELETE FROM leaderboard_entries")
    suspend fun clear()
}

