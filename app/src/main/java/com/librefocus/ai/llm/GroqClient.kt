package com.librefocus.ai.llm

import com.librefocus.data.IApiKeyProvider
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.system.measureTimeMillis

/**
 * Groq-backed LLM client. Calls the Groq OpenAI-compatible chat completions endpoint.
 * The API key is fetched from the provided `IApiKeyProvider` by the provider name "groq".
 */
class GroqClient(
    private val apiKeyProvider: IApiKeyProvider? = null
) : LLMClient {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    private fun extractAssistantFromJson(raw: String): String? {
        try {
            val elem = json.parseToJsonElement(raw)

            fun findInObject(obj: JsonObject): String? {
                listOf("choices", "output", "result", "response", "message", "data", "text", "content").forEach { key ->
                    val v = obj[key]
                    if (v != null) {
                        // choices array -> first -> message.content or text
                        if (key == "choices" && v is JsonArray) {
                            val first = v.firstOrNull()
                            if (first is JsonObject) {
                                val msg = first["message"]?.let { m -> (m as? JsonObject)?.get("content") }
                                val t = msg ?: first["text"] ?: first["content"]
                                if (t is JsonPrimitive && t.isString) return t.content
                            }
                        }

                        if (v is JsonPrimitive && v.isString) return v.content
                        if (v is JsonObject) {
                            val nested = findInObject(v)
                            if (!nested.isNullOrBlank()) return nested
                        }
                        if (v is JsonArray) {
                            val fromArr = v.mapNotNull { item ->
                                if (item is JsonPrimitive) {
                                    if (item.isString) item.content else null
                                } else if (item is JsonObject) {
                                    findInObject(item)
                                } else null
                            }.firstOrNull { it.isNotBlank() }
                            if (!fromArr.isNullOrBlank()) return fromArr
                        }
                    }
                }
                return null
            }

            if (elem is JsonObject) {
                return findInObject(elem)
            }
        } catch (_: Exception) {
            // ignore parse errors
        }
        return null
    }

    override suspend fun sendPrompt(prompt: LlmPrompt): LlmResponse {
        var raw = ""
        var structured: String? = null
        var err: String? = null
        val latency = measureTimeMillis {
            try {
                // Strictly target Groq's OpenAI-compatible endpoint (no proxy mapping)
                val url = "https://api.groq.com/openai/v1/chat/completions"

                // Build body: if prompt.messages exists, use OpenAI-compatible 'model' and 'messages' array
                val body = if (prompt.messages.isNotEmpty()) {
                    buildJsonObject {
                        put("model", prompt.metadata["model"] ?: "gpt-4o-mini")
                        putJsonArray("messages") {
                            // convert ChatMessage to JSON objects
                            prompt.messages.forEach { m ->
                                add(buildJsonObject { put("role", m.role); put("content", m.content) })
                            }
                        }
                    }
                } else {
                    // Fallback legacy body
                    buildJsonObject {
                        put("system", prompt.system)
                        put("user", prompt.user)
                        put("metadata", buildJsonObject { prompt.metadata.forEach { (k, v) -> put(k, v) } })
                    }
                }

                val response: HttpResponse = client.post(url) {
                    contentType(ContentType.Application.Json)
                    // Use stored user key for Groq
                    val key = apiKeyProvider?.getKey("groq") ?: return LlmResponse(rawText = "", structuredJson = null, latencyMs = 0, error = "Missing Groq API key")
                    header("Authorization", "Bearer $key")
                    setBody(body)
                }

                raw = response.bodyAsText()
                structured = raw

                if (raw.isBlank()) {
                    err = "Empty response from LLM (status=${response.status.value})"
                }
            } catch (e: Exception) {
                err = e.message ?: "Unknown network error"
            }
        }

        val extracted = if (!structured.isNullOrBlank()) extractAssistantFromJson(structured) else null
        val finalText = extracted ?: raw

        return LlmResponse(rawText = finalText, structuredJson = structured, latencyMs = latency, error = err)
    }
}
