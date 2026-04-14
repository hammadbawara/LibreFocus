package com.librefocus.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.librefocus.data.local.AppInfoProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppRepositoryTest {

    private lateinit var context: Context
    private lateinit var packageManager: PackageManager
    private lateinit var appInfoProvider: AppInfoProvider
    private lateinit var repository: AppRepository

    @Before
    fun setup() {
        context = mockk()
        packageManager = mockk()
        appInfoProvider = mockk()

        every { context.packageManager } returns packageManager
        
        repository = AppRepository(context, appInfoProvider)
    }

    @Test
    fun `isAppInstalled returns true when provider says it is installed`() = runTest {
        coEvery { appInfoProvider.isAppInstalled("com.test.app") } returns true
        
        assertTrue(repository.isAppInstalled("com.test.app"))
    }

    @Test
    fun `isAppInstalled returns false when provider says it is not installed`() = runTest {
        coEvery { appInfoProvider.isAppInstalled("com.missing.app") } returns false
        
        assertFalse(repository.isAppInstalled("com.missing.app"))
    }

    @Test
    fun `getAppName returns name from provider`() = runTest {
        coEvery { appInfoProvider.getAppName("com.test.app") } returns "Test App"
        
        assertEquals("Test App", repository.getAppName("com.test.app"))
    }

    @Test
    fun `getAppIcon returns icon when package exists`() = runTest {
        val mockDrawable = mockk<Drawable>()
        every { packageManager.getApplicationIcon("com.test.app") } returns mockDrawable

        val icon = repository.getAppIcon("com.test.app")
        
        assertEquals(mockDrawable, icon)
    }

    @Test
    fun `getAppIcon returns null when package not found`() = runTest {
        every { packageManager.getApplicationIcon("com.missing.app") } throws PackageManager.NameNotFoundException()

        val icon = repository.getAppIcon("com.missing.app")
        
        assertEquals(null, icon)
    }

    @Test
    fun `getInstalledApps maps output correctly and filters system apps`() = runTest {
        val userAppInfo = mockk<ApplicationInfo> {
            flags = 0
            packageName = "com.user.app"
        }
        val systemAppInfo = mockk<ApplicationInfo> {
            flags = ApplicationInfo.FLAG_SYSTEM
            packageName = "com.system.app"
        }

        every { packageManager.getInstalledApplications(PackageManager.GET_META_DATA) } returns listOf(userAppInfo, systemAppInfo)
        
        every { packageManager.getApplicationLabel(userAppInfo) } returns "User App"
        val mockIcon = mockk<Drawable>()
        every { packageManager.getApplicationIcon(userAppInfo) } returns mockIcon
        
        coEvery { appInfoProvider.getAppCategory("com.user.app") } returns "Productivity"

        val apps = repository.getInstalledApps()

        assertEquals(1, apps.size)
        assertEquals("User App", apps[0].appName)
        assertEquals("com.user.app", apps[0].packageName)
        assertEquals("Productivity", apps[0].category)
        assertEquals(mockIcon, apps[0].icon)
    }
}
