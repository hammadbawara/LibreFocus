package com.librefocus.data.local.database.dao

import androidx.room.*
import com.librefocus.data.local.database.entity.AppCategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for app categories.
 */
@Dao
interface AppCategoryDao {
    
    @Query("SELECT * FROM app_categories ORDER BY categoryName ASC")
    fun getAllCategories(): Flow<List<AppCategoryEntity>>
    
    @Query("SELECT * FROM app_categories WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: Int): AppCategoryEntity?
    
    @Query("SELECT * FROM app_categories WHERE categoryName = :categoryName LIMIT 1")
    suspend fun getCategoryByName(categoryName: String): AppCategoryEntity?
    
    @Query("SELECT * FROM app_categories WHERE isCustom = :isCustom")
    fun getCategoriesByType(isCustom: Boolean): Flow<List<AppCategoryEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: AppCategoryEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<AppCategoryEntity>): List<Long>
    
    @Update
    suspend fun updateCategory(category: AppCategoryEntity)
    
    @Delete
    suspend fun deleteCategory(category: AppCategoryEntity)
    
    @Query("DELETE FROM app_categories WHERE id = :categoryId")
    suspend fun deleteCategoryById(categoryId: Int)
}
