package com.librefocus.ai.llm

interface LLMClientProvider {
    fun getClient(provider: String): LLMClient
}

