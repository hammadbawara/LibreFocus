package com.librefocus.ai.llm

import com.librefocus.data.IApiKeyProvider
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

val GROQ_SUPPORTED_MODELS = setOf(
    "llama-3.3-70b-versatile",
    "llama-3.1-8b-instant",
    "meta-llama/llama-4-scout-17b-16e-instruct",
    "openai/gpt-oss-20b",
    "openai/gpt-oss-120b",
    "qwen/qwen3-32b"
)

val GEMINI_SUPPORTED_MODELS = setOf(
    "gemini-1.5-pro",
    "gemini-1.5-flash",
    "gemini-1.5-flash-8b",
    "gemini-2.0-flash",
    "gemini-2.0-flash-lite"
)

/**
 * Best-effort provider model fetcher. Uses stored API keys from IApiKeyProvider and queries provider endpoints
 * to retrieve available models. Falls back to hardcoded list on errors.
 */
class ProviderModelFetcher(private val apiKeyProvider: IApiKeyProvider?) {

    private val client = HttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchModels(provider: String): List<String> {
        return try {
            when (provider.lowercase()) {
                "groq" -> fetchGroqModels()
                "gemini" -> fetchGeminiModels()
                else -> emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchGroqModels(): List<String> {
        val base = GROQ_SUPPORTED_MODELS.toList()
        val key = apiKeyProvider?.getKey("groq") ?: return base
        val response = client.get("https://api.groq.com/openai/v1/models") {
            header("Authorization", "Bearer $key")
        }
        val body = response.bodyAsText()
        val elem =
            runCatching { json.parseToJsonElement(body) }.getOrNull() as? JsonObject ?: return base
        val data = elem["data"] as? JsonArray ?: return base
        val discovered = data.mapNotNull { (it as? JsonObject)?.get("id")?.toString()?.trim('"') }
            .filter { GROQ_SUPPORTED_MODELS.contains(it) }
        return (discovered + base).distinct()
    }

    private suspend fun fetchGeminiModels(): List<String> {
        val base = GEMINI_SUPPORTED_MODELS.toList()
        val key = apiKeyProvider?.getKey("gemini") ?: return base

        // Gemini list endpoint uses key as query param, not Bearer token
        val response = client.get(
            "https://generativelanguage.googleapis.com/v1beta/models?key=$key"
        )
        val body = response.bodyAsText()
        val elem =
            runCatching { json.parseToJsonElement(body) }.getOrNull() as? JsonObject ?: return base
        val models = elem["models"] as? JsonArray ?: return base

        // API returns names like "models/gemini-1.5-flash" — strip the prefix
        val discovered = models.mapNotNull {
            (it as? JsonObject)?.get("name")?.toString()
                ?.trim('"')
                ?.removePrefix("models/")
        }.filter { GEMINI_SUPPORTED_MODELS.contains(it) }

        return (discovered + base).distinct()
    }
}