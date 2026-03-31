package com.librefocus.di

import com.librefocus.ui.chatbot.ChatContextProvider
import com.librefocus.ui.chatbot.ChatViewModel
import com.librefocus.ui.chatbot.IChatContextProvider
import org.koin.dsl.module

val chatbotModule = module {
    single<IChatContextProvider> {
        ChatContextProvider(
            dailyDeviceUsageDao = get(),
            hourlyAppUsageDao = get(),
            appDao = get()
        )
    }
    factory { ChatViewModel(get()) }
}

// To change the model globally, update the factory to: factory { ChatViewModel(get(), model = "groq-7b") }
