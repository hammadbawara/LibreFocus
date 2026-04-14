package com.librefocus.data.local.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.librefocus.data.local.database.UsageDatabase
import com.librefocus.data.local.database.entity.AppCategoryEntity
import com.librefocus.data.local.database.entity.AppEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDaoTest {

    private lateinit var db: UsageDatabase
    private lateinit var appDao: AppDao
    private lateinit var categoryDao: AppCategoryDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, UsageDatabase::class.java
        ).build()
        appDao = db.appDao()
        categoryDao = db.appCategoryDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetApp() = runBlocking {
        // 1. Insert a category first (Foreign Key constraint)
        val category = AppCategoryEntity(
            categoryName = "Productivity",
            isCustom = false,
            addedAtUtc = System.currentTimeMillis()
        )
        val categoryId = categoryDao.insertCategory(category).toInt()

        // 2. Insert an app
        val app = AppEntity(
            packageName = "com.example.app",
            appName = "Example App",
            categoryId = categoryId
        )
        val appId = appDao.insertApp(app).toInt()

        // 3. Retrieve the app
        val retrievedApp = appDao.getAppById(appId)
        
        assertNotNull(retrievedApp)
        assertEquals("com.example.app", retrievedApp?.packageName)
        assertEquals("Example App", retrievedApp?.appName)
        assertEquals(categoryId, retrievedApp?.categoryId)
    }

    @Test
    fun getAllAppsOrderByAppName() = runBlocking {
        // Insert category
        val categoryId = categoryDao.insertCategory(
            AppCategoryEntity(categoryName = "Social", isCustom = true, addedAtUtc = 0)
        ).toInt()

        // Insert apps
        appDao.insertApp(AppEntity(packageName = "com.b", appName = "B App", categoryId = categoryId))
        appDao.insertApp(AppEntity(packageName = "com.a", appName = "A App", categoryId = categoryId))
        appDao.insertApp(AppEntity(packageName = "com.c", appName = "C App", categoryId = categoryId))

        // Get all apps flow
        val allApps = appDao.getAllApps().first()

        assertEquals(3, allApps.size)
        // Verify order
        assertEquals("A App", allApps[0].appName)
        assertEquals("B App", allApps[1].appName)
        assertEquals("C App", allApps[2].appName)
    }

    @Test
    fun deleteApp() = runBlocking {
        val categoryId = categoryDao.insertCategory(
            AppCategoryEntity(categoryName = "Games", isCustom = true, addedAtUtc = 0)
        ).toInt()

        val appId = appDao.insertApp(
            AppEntity(packageName = "com.game", appName = "A Game", categoryId = categoryId)
        ).toInt()

        // Verify inserted
        assertTrue(appDao.isAppExists("com.game"))

        // Delete app
        appDao.deleteAppById(appId)

        // Verify deleted
        val isExists = appDao.isAppExists("com.game")
        assertEquals(false, isExists)
    }
}
