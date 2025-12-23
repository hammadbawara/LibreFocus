package com.librefocus.data.repository

import com.librefocus.data.local.database.dao.AppCategoryDao
import com.librefocus.data.local.database.dao.AppDao
import com.librefocus.data.local.database.entity.AppCategoryEntity
import com.librefocus.data.local.database.entity.AppEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Repository for managing app categories and their relationships with apps.
 */
class CategoryRepository(
    private val appCategoryDao: AppCategoryDao,
    private val appDao: AppDao
) {

    fun getAllCategories(): Flow<List<AppCategoryEntity>> {
        return appCategoryDao.getAllCategories()
    }

    suspend fun getCategoryById(categoryId: Int): AppCategoryEntity? {
        return appCategoryDao.getCategoryById(categoryId)
    }
    

    suspend fun getCategoryByName(categoryName: String): AppCategoryEntity? {
        return appCategoryDao.getCategoryByName(categoryName)
    }

    fun getAppsByCategory(categoryId: Int): Flow<List<AppEntity>> {
        return appDao.getAppsByCategory(categoryId)
    }

    suspend fun getAppCountInCategory(categoryId: Int): Int {
        return appDao.getAppsByCategory(categoryId).first().size
    }

    suspend fun insertCategory(category: AppCategoryEntity): Long {
        // Check if category name already exists
        val existing = appCategoryDao.getCategoryByName(category.categoryName)
        if (existing != null) {
            throw CategoryAlreadyExistsException("Category '${category.categoryName}' already exists")
        }
        
        return appCategoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: AppCategoryEntity) {
        // Check if the new name conflicts with another category
        val existing = appCategoryDao.getCategoryByName(category.categoryName)
        if (existing != null && existing.id != category.id) {
            throw CategoryAlreadyExistsException("Category '${category.categoryName}' already exists")
        }
        
        appCategoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(categoryId: Int, uncategorizedCategoryId: Int? = null) {
        val appsInCategory = appDao.getAppsByCategory(categoryId).first()
        
        if (appsInCategory.isNotEmpty()) {
            if (uncategorizedCategoryId == null) {
                throw CategoryNotEmptyException("Cannot delete category with ${appsInCategory.size} apps")
            }
            
            // Move all apps to uncategorized
            appsInCategory.forEach { app ->
                appDao.updateApp(app.copy(categoryId = uncategorizedCategoryId))
            }
        }
        
        appCategoryDao.deleteCategoryById(categoryId)
    }

    suspend fun removeAppFromCategory(appId: Int, newCategoryId: Int) {
        val app = appDao.getAppById(appId)
        if (app != null) {
            appDao.updateApp(app.copy(categoryId = newCategoryId))
        }
    }

    suspend fun updateAppCategory(appId: Int, categoryId: Int) {
        val app = appDao.getAppById(appId)
        if (app != null) {
            appDao.updateApp(app.copy(categoryId = categoryId))
        }
    }

    suspend fun initializeSystemCategories() {
        val existingCategories = appCategoryDao.getAllCategories().first()
        
        if (existingCategories.isEmpty()) {
            val systemCategories = listOf(
                "Undefined",
                "Game",
                "Audio",
                "Video",
                "Image",
                "Social",
                "News",
                "Maps",
                "Productivity",
                "Accessibility"
            )
            
            val currentTime = System.currentTimeMillis()
            val categoriesToInsert = systemCategories.map { name ->
                AppCategoryEntity(
                    categoryName = name,
                    isCustom = false,
                    addedAtUtc = currentTime
                )
            }
            
            appCategoryDao.insertCategories(categoriesToInsert)
        }
    }
}

class CategoryAlreadyExistsException(message: String) : Exception(message)


class CategoryNotEmptyException(message: String) : Exception(message)
