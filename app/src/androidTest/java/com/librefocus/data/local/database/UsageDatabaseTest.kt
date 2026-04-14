package com.librefocus.data.local.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UsageDatabaseTest {

    private lateinit var db: UsageDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, UsageDatabase::class.java
        ).build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun databaseCreationIsSuccessful() {
        assertNotNull(db)
        assertNotNull(db.appDao())
        assertNotNull(db.appCategoryDao())
        assertNotNull(db.dailyDeviceUsageDao())
        assertNotNull(db.limitDao())
    }
}
