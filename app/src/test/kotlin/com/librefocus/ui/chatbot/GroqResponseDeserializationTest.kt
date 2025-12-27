package com.librefocus.ui.chatbot

import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.Assert.assertTrue

class GroqResponseDeserializationTest {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun missingChoices_defaultsToEmptyList() {
        val s = "{}"
        val resp = json.decodeFromString<GroqResponse>(s)
        assertTrue(resp.choices.isEmpty())
    }
}
