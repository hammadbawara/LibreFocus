package com.librefocus.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.librefocus.data.local.database.entity.BadgeEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for badges/achievements.
 */
@Dao
interface BadgeDao {
    
    @Query("SELECT * FROM badges ORDER BY id ASC")
    fun getAllBadges(): Flow<List<BadgeEntity>>
    
    @Query("SELECT * FROM badges ORDER BY id ASC")
    suspend fun getAllBadgesOnce(): List<BadgeEntity>
    
    @Query("SELECT * FROM badges WHERE isUnlocked = 1 ORDER BY unlockedAt DESC")
    fun getUnlockedBadges(): Flow<List<BadgeEntity>>
    
    @Query("SELECT * FROM badges WHERE isUnlocked = 1 ORDER BY unlockedAt DESC")
    suspend fun getUnlockedBadgesOnce(): List<BadgeEntity>
    
    @Query("SELECT * FROM badges WHERE id = :badgeId LIMIT 1")
    suspend fun getBadgeById(badgeId: Int): BadgeEntity?
    
    @Query("SELECT COUNT(*) FROM badges WHERE isUnlocked = 1")
    suspend fun getUnlockedBadgeCount(): Int
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBadges(badges: List<BadgeEntity>)
    
    @Update
    suspend fun updateBadge(badge: BadgeEntity)
    
    @Delete
    suspend fun deleteBadge(badge: BadgeEntity)
    
    @Query("DELETE FROM badges")
    suspend fun clear()
}

