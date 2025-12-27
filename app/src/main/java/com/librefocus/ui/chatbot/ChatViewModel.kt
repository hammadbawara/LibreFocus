package com.librefocus.ui.chatbot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.librefocus.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonArray

data class Message(val text: String, val isFromUser: Boolean)

@Serializable
data class GroqRequest(
    val model: String,
    val messages: List<GroqMessage>
)

@Serializable
data class GroqMessage(
    val role: String,
    val content: String
)

@Serializable
data class GroqResponse(
    val choices: List<Choice> = emptyList()
)

@Serializable
data class Choice(
    val message: GroqMessage? = null,
    val text: String? = null
)

open class ChatViewModel(private val chatContextProvider: IChatContextProvider, private val model: String = "llama-3.3-70b-versatile") : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    // Shared Json instance used by both the HTTP client and manual decoding
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // Try multiple strategies to extract a human-readable assistant response from raw JSON
    private fun extractAssistantText(raw: String): String? {
        // 1) Preferred: proper GroqResponse shape
        try {
            val gr = json.decodeFromString(GroqResponse.serializer(), raw)
            val choice = gr.choices.firstOrNull()
            val maybe = choice?.message?.content ?: choice?.text
            if (!maybe.isNullOrBlank()) return maybe
        } catch (_: Exception) {
            // fall through
        }

        // 2) Parse generically and look for common keys
        val elem = try {
            json.parseToJsonElement(raw)
        } catch (_: Exception) {
            return null
        }

        fun findInObject(obj: JsonObject): String? {
            // common direct paths
            listOf("choices", "output", "result", "response", "message", "data", "text", "content").forEach { key ->
                val v = obj[key]
                if (v != null) {
                    // choices array
                    if (key == "choices" && v is JsonArray) {
                        val first = v.firstOrNull()
                        if (first is JsonObject) {
                            // try message.content
                            val msg = first["message"]?.let { m -> (m as? JsonObject)?.get("content") }
                            val t = msg ?: first["text"] ?: first["content"]
                            if (t is JsonPrimitive && t.isString) return t.content
                        }
                    }

                    // direct string
                    if (v is JsonPrimitive && v.isString) return v.content
                    if (v is JsonObject) {
                        val nested = findInObject(v)
                        if (!nested.isNullOrBlank()) return nested
                    }
                    if (v is JsonArray) {
                        val fromArr = v.mapNotNull { item ->
                            when (item) {
                                is JsonPrimitive -> if (item.isString) item.content else null
                                is JsonObject -> findInObject(item)
                                is JsonArray -> null
                            }
                        }.firstOrNull { it.isNotBlank() }
                        if (!fromArr.isNullOrBlank()) return fromArr
                    }
                }
            }

            return null
        }

        fun findLongestString(elem: JsonElement): String? {
            return when (elem) {
                is JsonPrimitive -> if (elem.isString) elem.content else null
                is JsonObject -> elem.values.mapNotNull { findLongestString(it) }.maxByOrNull { it.length }
                is JsonArray -> elem.mapNotNull { findLongestString(it) }.maxByOrNull { it.length }
            }
        }

        if (elem is JsonObject) {
            val byKey = findInObject(elem)
            if (!byKey.isNullOrBlank()) return byKey
        }

        // Fallback: largest string anywhere in document
        val longest = findLongestString(elem)
        return if (!longest.isNullOrBlank()) longest else null
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    override fun onCleared() {
        super.onCleared()
        client.close()
    }

    open fun sendMessage(text: String) {
        _messages.value = _messages.value + Message(text, true)

        viewModelScope.launch {
            try {
                val context = chatContextProvider.getBehaviorContext()
                val response = client.post("https://api.groq.com/openai/v1/chat/completions") {
                    header("Authorization", "Bearer ${BuildConfig.GROQ_API_KEY}")
                    contentType(ContentType.Application.Json)
                    setBody(GroqRequest(
                        model = model,
                        messages = listOf(
                            GroqMessage("system", "You are a supportive digital wellbeing assistant. Here is the user's context:\n$context"),
                            GroqMessage("user", text)
                        )
                    ))
                }

                // Read raw response as text for parsing
                val raw = response.bodyAsText()

                // Try manual deserialization so we can catch and report parse errors
                val groqResponse = try {
                    json.decodeFromString(GroqResponse.serializer(), raw)
                } catch (_: Exception) {
                    null
                }

                val botResponse = extractAssistantText(raw)
                    ?: groqResponse?.choices?.firstOrNull()?.message?.content
                    ?: groqResponse?.choices?.firstOrNull()?.text
                    ?: "(No response from assistant)"
                _messages.value = _messages.value + Message(botResponse, false)
            } catch (e: Exception) {
                _messages.value = _messages.value + Message("Error: ${e.message}", false)
            }
        }
    }
}
