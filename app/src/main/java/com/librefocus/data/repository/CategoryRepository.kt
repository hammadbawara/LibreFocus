package com.librefocus.data.repository

import android.content.pm.ApplicationInfo
import com.librefocus.data.local.database.dao.AppCategoryDao
import com.librefocus.data.local.database.dao.AppDao
import com.librefocus.data.local.database.entity.AppCategoryEntity
import com.librefocus.data.local.database.entity.AppEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

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
    
    suspend fun getCategoryBySystemId(systemCategoryId: Int): AppCategoryEntity? {
        return appCategoryDao.getCategoryBySystemId(systemCategoryId)
    }

    fun getAppsByCategory(categoryId: Int): Flow<List<AppEntity>> {
        return appDao.getAppsByCategory(categoryId)
    }

    suspend fun getAppCountInCategory(categoryId: Int): Int {
        return appDao.getAppsByCategory(categoryId).first().size
    }

    suspend fun insertCategory(category: AppCategoryEntity): Long {
        val existing = appCategoryDao.getCategoryByName(category.categoryName)
        if (existing != null) {
            throw CategoryAlreadyExistsException("Category '${category.categoryName}' already exists")
        }
        
        return appCategoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: AppCategoryEntity) {
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
                ApplicationInfo.CATEGORY_UNDEFINED to "Undefined",
                ApplicationInfo.CATEGORY_GAME to "Game",
                ApplicationInfo.CATEGORY_AUDIO to "Audio",
                ApplicationInfo.CATEGORY_VIDEO to "Video",
                ApplicationInfo.CATEGORY_IMAGE to "Image",
                ApplicationInfo.CATEGORY_SOCIAL to "Social",
                ApplicationInfo.CATEGORY_NEWS to "News",
                ApplicationInfo.CATEGORY_MAPS to "Maps",
                ApplicationInfo.CATEGORY_PRODUCTIVITY to "Productivity",
                ApplicationInfo.CATEGORY_ACCESSIBILITY to "Accessibility"
            )
            
            val currentTime = System.currentTimeMillis()
            val categoriesToInsert = systemCategories.map { (id, fallbackName) ->
                AppCategoryEntity(
                    categoryName = fallbackName,
                    isCustom = false,
                    systemCategoryId = id,
                    addedAtUtc = currentTime
                )
            }
            
            appCategoryDao.insertCategories(categoriesToInsert)
        }
    }
}

class CategoryAlreadyExistsException(message: String) : Exception(message)
class CategoryNotEmptyException(message: String) : Exception(message)
