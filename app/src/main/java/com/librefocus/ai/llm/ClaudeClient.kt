package com.librefocus.ai.llm

import com.librefocus.data.IApiKeyProvider

/**
 * Minimal stub client for Anthropic Claude (non-functional placeholder).
 * Returns a friendly message indicating the provider is not yet implemented.
 */
class ClaudeClient(private val apiKeyProvider: IApiKeyProvider? = null) : LLMClient {
    override suspend fun sendPrompt(prompt: LlmPrompt): LlmResponse {
        // Minimal stub: do not perform network calls here.
        val msg = "(Claude client not implemented in this build)"
        return LlmResponse(rawText = msg, structuredJson = null, latencyMs = 0L, error = "Not implemented")
    }
}

