package com.librefocus.ai

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnonymizerTest {

    @Test
    fun computeUserHash_isDeterministicAndTruncated() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val secret = kotlin.runBlocking { Anonymizer.getOrCreateSecret(ctx) }
        val rawId = "user-12345"
        val hash1 = Anonymizer.computeUserHash(secret, rawId)
        val hash2 = Anonymizer.computeUserHash(secret, rawId)
        assertEquals(hash1, hash2)
        assertTrue(hash1.length == 12)
    }

    @Test
    fun validateSummaryKeys_allowsOnlyWhitelisted() {
        val map = mapOf(
            "userHash" to "abc",
            "daysObserved" to 5,
            "dailyMeanMinutes" to 120.0
        )
        assertTrue(Anonymizer.validateSummaryKeys(map))

        val bad = map.plus("packageNames" to listOf("com.example"))
        assertTrue(!Anonymizer.validateSummaryKeys(bad))
    }
}

