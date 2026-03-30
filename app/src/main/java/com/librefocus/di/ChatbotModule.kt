package com.librefocus.di

import com.librefocus.ui.chatbot.ChatContextProvider
import com.librefocus.ui.chatbot.ChatViewModel
import com.librefocus.ui.chatbot.IChatContextProvider
import com.librefocus.ui.chatbot.IChatViewModel
import com.librefocus.data.ApiKeyProvider
import com.librefocus.data.IApiKeyProvider
import com.librefocus.ai.llm.GroqClient
import com.librefocus.ai.llm.LLMClient
import com.librefocus.ai.llm.LLMClientProvider
import com.librefocus.ai.llm.GeminiClient
import com.librefocus.data.repository.ChatRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val chatbotModule = module {
    single<IChatContextProvider> {
        // Provide the AI-focused ChatContextProvider which builds a richer behavioral summary
        ChatContextProvider(
            dailyDeviceUsageDao = get(),
            hourlyAppUsageDao = get(),
            appDao = get()
        )
    }

    // Provide an API key provider that uses EncryptedSharedPreferences (fallback for user-supplied key)
    single<IApiKeyProvider> {
        ApiKeyProvider(androidContext())
    }

    // Provide provider-specific LLM clients (register under the LLMClient type so qualifiers resolve by type+qualifier)
    single<LLMClient>(named("groqClient")) {
        GroqClient(get())
    }

    single<LLMClient>(named("geminiClient")) {
        GeminiClient(get())
    }

    // Expose an LLMClientProvider that maps provider keys to concrete clients
    single<LLMClientProvider> {
        val groq: LLMClient = get(named("groqClient"))
        val gemini: LLMClient = get(named("geminiClient"))
        object : LLMClientProvider {
            override fun getClient(provider: String): LLMClient {
                return when (provider.lowercase()) {
                    "gemini" -> gemini
                    // default to groq for unknown providers for now
                    else -> groq
                }
            }
        }
    }

    // Chat storage repository
    single {
        ChatRepository(get())
    }

    // Bind the ChatViewModel to the IChatViewModel interface for DI into composables
    factory<IChatViewModel> { ChatViewModel(get(), get(), get(), get(), get()) }

    // Also keep a concrete factory for the concrete type (if needed elsewhere)
    factory { ChatViewModel(get(), get(), get(), get(), get()) }
}

// To change the model globally, update the factory to: factory { ChatViewModel(get(), get(), get(), model = "groq-7b") }
