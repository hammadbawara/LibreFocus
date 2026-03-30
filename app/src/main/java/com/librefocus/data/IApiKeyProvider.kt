package com.librefocus.data

interface IApiKeyProvider {
    fun saveKey(provider: String, key: String)
    fun getKey(provider: String): String?
    fun clearKey(provider: String)
}
