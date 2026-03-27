package com.librefocus.ai.llm

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class LlmPrompt(
    val system: String = "",
    val user: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class LlmResponse(
    val rawText: String,
    val structuredJson: String? = null,
    val latencyMs: Long = 0,
    val error: String? = null
)
