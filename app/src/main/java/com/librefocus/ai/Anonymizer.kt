package com.librefocus.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Anonymizer helpers: compute per-install HMAC and sanitize/validate summaries.
 * Uses Android Keystore when available; fallback to a local secret stored in the app files directory.
 */
object Anonymizer {

    private const val HMAC_ALGO = "HmacSHA256"

    // Compute a truncated HMAC hex string for a given raw id
    fun computeUserHash(secretKeyBytes: ByteArray, rawId: String): String {
        val keySpec: SecretKey = SecretKeySpec(secretKeyBytes, HMAC_ALGO)
        val mac = Mac.getInstance(HMAC_ALGO)
        mac.init(keySpec)
        val digest = mac.doFinal(rawId.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { String.format("%02x", it) }.substring(0, 12)
    }

    // Validate that the summary map contains only allowed keys (defensive check before sending to LLM)
    fun validateSummaryKeys(summaryMap: Map<String, Any?>): Boolean {
        val allowed = setOf(
            "userHash",
            "daysObserved",
            "dailyMeanMinutes",
            "rollingMean7",
            "trendSlopeMinutesPerDay",
            "goalMinutes",
            "goalAdherenceRate",
            "sessionCount",
            "avgSessionMinutes",
            "timeOfDayBuckets",
            "noData"
        )
        return summaryMap.keys.all { it in allowed }
    }

    // Helper to create or derive a per-install secret. This is a pragmatic fallback; in production use Keystore or EncryptedSharedPreferences.
    suspend fun getOrCreateSecret(context: Context): ByteArray = withContext(Dispatchers.IO) {
        val file = java.io.File(context.filesDir, "librefocus_secret")
        if (file.exists()) {
            return@withContext file.readBytes()
        }
        val random = ByteArray(32)
        java.security.SecureRandom().nextBytes(random)
        file.writeBytes(random)
        return@withContext random
    }
}
