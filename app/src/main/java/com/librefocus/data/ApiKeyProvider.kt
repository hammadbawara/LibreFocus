package com.librefocus.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

private const val PREFS_NAME = "ai_keys_prefs"
private const val PREF_KEY_PREFIX = "api_key_"

/**
 * API key provider using EncryptedSharedPreferences backed by Android Keystore when available.
 * Falls back to regular SharedPreferences if encrypted prefs cannot be created (very rare).
 */
class ApiKeyProvider(private val context: Context) : IApiKeyProvider {

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // If EncryptedSharedPreferences isn't available for any reason, fall back to regular prefs
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    override fun saveKey(provider: String, key: String) {
        prefs.edit().putString(PREF_KEY_PREFIX + provider, key).apply()
    }

    override fun getKey(provider: String): String? = prefs.getString(PREF_KEY_PREFIX + provider, null)

    override fun clearKey(provider: String) {
        prefs.edit().remove(PREF_KEY_PREFIX + provider).apply()
    }
}
