package com.librefocus.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.librefocus.data.local.database.entity.ChallengeEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for daily challenges.
 */
@Dao
interface ChallengeDao {
    
    @Query("SELECT * FROM challenges ORDER BY date DESC")
    fun getAllChallenges(): Flow<List<ChallengeEntity>>
    
    @Query("SELECT * FROM challenges WHERE date = :date ORDER BY id ASC")
    fun getChallengesForDate(date: Long): Flow<List<ChallengeEntity>>
    
    @Query("SELECT * FROM challenges WHERE date = :date ORDER BY id ASC")
    suspend fun getChallengesForDateOnce(date: Long): List<ChallengeEntity>
    
    @Query("SELECT * FROM challenges WHERE date = :date AND isCompleted = 0")
    suspend fun getActiveChallengesForDate(date: Long): List<ChallengeEntity>
    
    @Query("SELECT * FROM challenges WHERE isCompleted = 0 ORDER BY date DESC LIMIT 1")
    suspend fun getLatestActiveChallenge(): ChallengeEntity?
    
    @Query("SELECT * FROM challenges WHERE id = :challengeId LIMIT 1")
    suspend fun getChallengeById(challengeId: Int): ChallengeEntity?
    
    @Query("SELECT COUNT(*) FROM challenges WHERE isCompleted = 1")
    suspend fun getCompletedChallengeCount(): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenge(challenge: ChallengeEntity)
    
    @Update
    suspend fun updateChallenge(challenge: ChallengeEntity)
    
    @Delete
    suspend fun deleteChallenge(challenge: ChallengeEntity)
    
    @Query("DELETE FROM challenges")
    suspend fun clear()
}

