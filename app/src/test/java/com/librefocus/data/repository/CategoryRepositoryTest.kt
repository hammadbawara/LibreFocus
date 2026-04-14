package com.librefocus.data.repository

import com.librefocus.data.local.database.dao.AppCategoryDao
import com.librefocus.data.local.database.dao.AppDao
import com.librefocus.data.local.database.entity.AppCategoryEntity
import com.librefocus.data.local.database.entity.AppEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CategoryRepositoryTest {

    private lateinit var appCategoryDao: AppCategoryDao
    private lateinit var appDao: AppDao
    private lateinit var repository: CategoryRepository

    @Before
    fun setup() {
        appCategoryDao = mockk()
        appDao = mockk()
        repository = CategoryRepository(appCategoryDao, appDao)
    }

    @Test
    fun `getAllCategories returns from dao`() = runTest {
        val categories = listOf(AppCategoryEntity(id = 1, categoryName = "Games", isCustom = true, addedAtUtc = 0L))
        coEvery { appCategoryDao.getAllCategories() } returns flowOf(categories)

        val result = repository.getAllCategories().first()
        assertEquals(1, result.size)
        assertEquals("Games", result[0].categoryName)
    }

    @Test
    fun `insertCategory succeeds if name does not exist`() = runTest {
        val category = AppCategoryEntity(id = 0, categoryName = "NewCat", isCustom = true, addedAtUtc = 0L)
        coEvery { appCategoryDao.getCategoryByName("NewCat") } returns null
        coEvery { appCategoryDao.insertCategory(category) } returns 10L

        val id = repository.insertCategory(category)
        
        assertEquals(10L, id)
        coVerify { appCategoryDao.insertCategory(category) }
    }

    @Test
    fun `insertCategory throws exception if name exists`() = runTest {
        val category = AppCategoryEntity(id = 0, categoryName = "Existing", isCustom = true, addedAtUtc = 0L)
        coEvery { appCategoryDao.getCategoryByName("Existing") } returns category

        var exceptionThrown = false
        try {
            repository.insertCategory(category)
        } catch (e: CategoryAlreadyExistsException) {
            exceptionThrown = true
        }
        org.junit.Assert.assertTrue(exceptionThrown)
        
        coVerify(exactly = 0) { appCategoryDao.insertCategory(any()) }
    }

    @Test
    fun `deleteCategory throws exception if category is not empty and no fallback provided`() = runTest {
        val app = AppEntity(id = 1, packageName = "test", appName = "Test", categoryId = 1)
        coEvery { appDao.getAppsByCategory(1) } returns flowOf(listOf(app))

        var exceptionThrown = false
        try {
            repository.deleteCategory(categoryId = 1, uncategorizedCategoryId = null)
        } catch (e: CategoryNotEmptyException) {
            exceptionThrown = true
        }
        org.junit.Assert.assertTrue(exceptionThrown)
        
        coVerify(exactly = 0) { appCategoryDao.deleteCategoryById(any()) }
    }

    @Test
    fun `deleteCategory uses fallback if category is not empty`() = runTest {
        val app = AppEntity(id = 1, packageName = "test", appName = "Test", categoryId = 1)
        coEvery { appDao.getAppsByCategory(1) } returns flowOf(listOf(app))
        coEvery { appDao.updateApp(any()) } returns Unit
        coEvery { appCategoryDao.deleteCategoryById(1) } returns Unit

        repository.deleteCategory(categoryId = 1, uncategorizedCategoryId = 2)
        
        coVerify { appDao.updateApp(match { it.categoryId == 2 }) }
        coVerify { appCategoryDao.deleteCategoryById(1) }
    }

    @Test
    fun `deleteCategory deletes category immediately if empty`() = runTest {
        coEvery { appDao.getAppsByCategory(1) } returns flowOf(emptyList())
        coEvery { appCategoryDao.deleteCategoryById(1) } returns Unit

        repository.deleteCategory(categoryId = 1, uncategorizedCategoryId = null)
        
        coVerify { appCategoryDao.deleteCategoryById(1) }
        coVerify(exactly = 0) { appDao.updateApp(any()) }
    }

    @Test
    fun `initializeSystemCategories populates when empty`() = runTest {
        coEvery { appCategoryDao.getAllCategories() } returns flowOf(emptyList())
        coEvery { appCategoryDao.insertCategories(any()) } returns emptyList()

        repository.initializeSystemCategories()
        
        coVerify { appCategoryDao.insertCategories(match { it.size == 10 }) }
    }

    @Test
    fun `initializeSystemCategories skips population when not empty`() = runTest {
        coEvery { appCategoryDao.getAllCategories() } returns flowOf(listOf(mockk()))

        repository.initializeSystemCategories()
        
        coVerify(exactly = 0) { appCategoryDao.insertCategories(any()) }
    }
}
