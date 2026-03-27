package com.librefocus.ai.llm

import com.librefocus.data.IApiKeyProvider
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlin.system.measureTimeMillis

/**
 * Concrete Gemini client that calls the Google Generative Language API (Gemini) using REST.
 */
class GeminiClient(private val apiKeyProvider: IApiKeyProvider? = null) : LLMClient {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false // Prevents null fields from being serialized into the request body
    }

    private val http = HttpClient {
        install(ContentNegotiation) { json(json) }
    }

    override suspend fun sendPrompt(prompt: LlmPrompt): LlmResponse {
        val key = apiKeyProvider?.getKey("gemini")
            ?: return LlmResponse(rawText = "", structuredJson = null, latencyMs = 0, error = "Missing Gemini API key")

        // Default to a free-tier model; always use v1beta (required for system_instruction and newer models)
        val model = prompt.metadata["model"] ?: "gemini-2.0-flash"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$key"

        val body = GeminiRequest(
            contents = buildContents(prompt, prompt.system),
            systemInstruction = null,
            generationConfig = GeminiGenerationConfig()
        )

        var raw = ""
        var err: String? = null
        val latency = measureTimeMillis {
            try {
                val response = http.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
                raw = response.bodyAsText()
            } catch (e: Exception) {
                err = e.message ?: "Gemini request failed"
            }
        }

        val text = if (raw.isNotBlank()) extractContent(raw) else null
        return LlmResponse(
            rawText = text ?: raw,
            structuredJson = raw.ifBlank { null },
            latencyMs = latency,
            error = err
        )
    }

    private fun buildContents(prompt: LlmPrompt, inlineSystem: String?): List<GeminiContent> {
        val messages = if (prompt.messages.isNotEmpty()) prompt.messages else listOf(
            ChatMessage(role = "user", content = prompt.user)
        )
        val systemText = inlineSystem?.takeIf { it.isNotBlank() }
        val adjusted = if (systemText.isNullOrBlank()) {
            messages
        } else {
            val hasUser = messages.any { it.role.lowercase() != "assistant" && it.role.lowercase() != "model" }
            if (hasUser) {
                messages.mapIndexed { index, message ->
                    if (index == 0 && message.role.lowercase() != "assistant" && message.role.lowercase() != "model") {
                        message.copy(content = "System:\n$systemText\n\n${message.content}")
                    } else {
                        message
                    }
                }
            } else {
                listOf(ChatMessage(role = "user", content = "System:\n$systemText")) + messages
            }
        }

        return adjusted.map { message ->
            val role = when (message.role.lowercase()) {
                "assistant", "model" -> "model"
                else -> "user"
            }
            GeminiContent(role = role, parts = listOf(GeminiPart(message.content)))
        }
    }

    private fun extractContent(raw: String): String? {
        return runCatching {
            val root = json.parseToJsonElement(raw)
            val candidates = (root as? JsonObject)?.get("candidates")
            val firstCandidate = (candidates as? JsonElement)?.jsonArray?.firstOrNull() as? JsonObject
            val content = firstCandidate?.get("content") as? JsonObject
            val parts = content?.get("parts")
            val textPart = (parts as? JsonElement)?.jsonArray?.firstOrNull() as? JsonObject
            (textPart?.get("text") as? JsonPrimitive)?.content
        }.getOrNull()
    }
}

@Serializable
private data class GeminiRequest(
    val contents: List<GeminiContent>,
    @SerialName("system_instruction") val systemInstruction: GeminiSystemInstruction? = null,
    @SerialName("generation_config") val generationConfig: GeminiGenerationConfig? = null
)

@Serializable
private data class GeminiContent(
    val role: String,
    val parts: List<GeminiPart>
)

@Serializable
private data class GeminiPart(@SerialName("text") val text: String)

@Serializable
private data class GeminiSystemInstruction(val parts: List<GeminiPart>)

@Serializable
private data class GeminiGenerationConfig(
    @SerialName("temperature") val temperature: Double? = 0.4,
    @SerialName("top_p") val topP: Double? = 0.95,
    @SerialName("max_output_tokens") val maxTokens: Int? = 2048
)