package com.librefocus.ai.llm

/**
 * Abstraction for an LLM client.
 */
interface LLMClient {
    suspend fun sendPrompt(prompt: LlmPrompt): LlmResponse
}

