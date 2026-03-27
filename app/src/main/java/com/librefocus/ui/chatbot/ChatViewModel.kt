package com.librefocus.ui.chatbot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.librefocus.ai.llm.ChatMessage
import com.librefocus.ai.llm.LLMClient
import com.librefocus.ai.llm.LLMClientProvider
import com.librefocus.ai.llm.LlmPrompt
import com.librefocus.ai.llm.LlmResponse
import com.librefocus.data.IApiKeyProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

data class Message(val text: String, val isFromUser: Boolean, val model: String? = null, val thought: String? = null)

open class ChatViewModel(
    private val chatContextProvider: IChatContextProvider,
    private val llmProvider: LLMClientProvider,
    private val chatRepository: com.librefocus.data.repository.ChatRepository,
    private val preferencesRepository: com.librefocus.data.repository.PreferencesRepository,
    private val apiKeyProvider: IApiKeyProvider,
    private var model: String = "llama-3.3-70b-versatile"
) : ViewModel(), IChatViewModel {

    private fun clampTitle(raw: String?, maxChars: Int = 48): String? {
        val trimmed = raw?.trim()?.ifBlank { null } ?: return null
        return if (trimmed.length <= maxChars) trimmed else trimmed.take(maxChars - 3) + "..."
    }

    private fun sanitizeCandidateTitle(raw: String?): String? {
        val trimmed = raw?.trim()?.ifBlank { null } ?: return null
        val suspicious = trimmed.startsWith("{") || trimmed.contains("\"error\"", ignoreCase = true) || trimmed.contains("error:", ignoreCase = true)
        if (suspicious) return null
        return clampTitle(trimmed)
    }

    private suspend fun loadStoredTitle(id: String): String? = runCatching {
        preferencesRepository.getConversationTitle(id).first()
    }.getOrNull()

    private suspend fun persistTitle(id: String, title: String?) {
        preferencesRepository.setConversationTitle(id, title)
        _currentConversationTitle.value = title
    }

    private fun hasApiKey(): Boolean = runCatching { apiKeyProvider.getKey(activeProvider) }.getOrNull().isNullOrBlank().not()

    private var awaitingInitialAiTitle: MutableMap<String, Boolean> = mutableMapOf()

    private suspend fun tryGenerateAiTitle(id: String, client: LLMClient): Boolean {
        if (!hasApiKey()) return false
        val recent = conversationHistory.takeLast(10).ifEmpty { return false }
        val conversationSummary = recent.joinToString("\n") { entry ->
            val role = entry.role.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            "$role: ${entry.content}"
        }.ifBlank { null }
        val promptInput = conversationSummary ?: "User question: ${_messages.value.firstOrNull { it.isFromUser }?.text ?: "New chat"}"
        val titlePrompt = LlmPrompt(
            system = "You generate short, vivid titles for chat conversations. Respond with 6 words max.",
            user = promptInput
        )
        val titleResp = runCatching { client.sendPrompt(titlePrompt) }.getOrNull() ?: return false
        val suggested = titleResp.rawText.takeIf { it.isNotBlank() } ?: titleResp.structuredJson?.let { extractAssistantText(it) }
        val candidate = sanitizeCandidateTitle(suggested)
        if (candidate.isNullOrBlank()) return false
        persistTitle(id, candidate)
        return true
    }

    private suspend fun applyFallbackTitle(id: String, text: String) {
        val fallback = clampTitle(text.lineSequence().firstOrNull()) ?: return
        persistTitle(id, fallback)
    }

    // Convenience constructor for previews/tests that don't have an LLMClient available.
    @Suppress("unused")
    constructor(chatContextProvider: IChatContextProvider) : this(
        chatContextProvider,
        object : LLMClientProvider {
            override fun getClient(provider: String): LLMClient = object : LLMClient { override suspend fun sendPrompt(prompt: LlmPrompt) = LlmResponse(rawText = "(noop)") }
        },
        com.librefocus.data.repository.ChatRepository(object : com.librefocus.data.local.database.dao.ChatMessageDao {
            override suspend fun insert(message: com.librefocus.data.local.database.entity.ChatMessageEntity): Long { return 0 }
            override suspend fun getMessagesForConversation(conversationId: String): List<com.librefocus.data.local.database.entity.ChatMessageEntity> = emptyList()
            override suspend fun deleteConversation(conversationId: String) {}
            override suspend fun getConversationSummaries(): List<com.librefocus.data.local.database.dao.ChatMessageDao.ConversationSummary> = emptyList()
        }),
        com.librefocus.data.repository.PreferencesRepository(com.librefocus.data.local.PreferencesDataStore(android.content.ContextWrapper(null))),
        object : IApiKeyProvider {
            override fun saveKey(provider: String, key: String) {}
            override fun getKey(provider: String): String? = null
            override fun clearKey(provider: String) {}
        }
    )

    private var conversationId: String? = null
    // Active provider (e.g., "groq", "openai", "anthropic"). Default to groq for now.
    private var activeProvider: String = "groq"

    // Keep an LLM-compatible conversation history separate from UI Message objects
    private val conversationHistory = mutableListOf<ChatMessage>()

    // Cap for non-system messages to keep requests small
    private val maxHistoryItems = 40 // counts user+assistant messages (non-system)

    // StateFlow backing field for UI messages and public contract implementation
    private val _messages: MutableStateFlow<List<Message>> = MutableStateFlow(emptyList())
    override val messages: StateFlow<List<Message>> get() = _messages

    // Conversations summary list and public flow
    private val _conversations: MutableStateFlow<List<ConversationSummary>> = MutableStateFlow(emptyList())
    override val conversations: StateFlow<List<ConversationSummary>> get() = _conversations

    // Current conversation title flow
    private val _currentConversationTitle: MutableStateFlow<String?> = MutableStateFlow(null)
    override val currentConversationTitle: StateFlow<String?> get() = _currentConversationTitle

    // Shared Json instance used by parsers
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    init {
        viewModelScope.launch {
            try {
                // load conversation summaries (will populate titles via refreshConversations)
                try { refreshConversations() } catch (_: Exception) {}

                // Start with a fresh empty conversation when the user opens the chat screen.
                // The user can select a previous conversation explicitly from the drawer; we will not auto-load the last one.
                val newId = chatRepository.newConversationId()
                conversationId = newId
                try { preferencesRepository.setLastConversationId(newId) } catch (_: Exception) {}
                _currentConversationTitle.value = preferencesRepository.getConversationTitle(newId).first()

                // Initialize provider/model for this new conversation from preferences (if any)
                try {
                    val prov = preferencesRepository.getConversationProvider(newId).first()
                    val mdl = preferencesRepository.getConversationModel(newId).first()
                    if (!prov.isNullOrBlank()) activeProvider = prov
                    if (!mdl.isNullOrBlank()) model = mdl
                } catch (_: Exception) {}

            } catch (_: Exception) {
                // ignore load errors
                if (conversationId == null) conversationId = try { chatRepository.newConversationId() } catch (_: Exception) { "" }
            }
        }
    }

    // Try multiple strategies to extract a human-readable assistant response from raw JSON
    private fun extractAssistantText(raw: String): String? {
        // 1) Try to find "message" or "choices"
        try {
            val elem = json.parseToJsonElement(raw)

            fun findInObject(obj: kotlinx.serialization.json.JsonObject): String? {
                listOf("choices", "output", "result", "response", "message", "data", "text", "content").forEach { key ->
                    val v = obj[key]
                    if (v != null) {
                        if (key == "choices" && v is kotlinx.serialization.json.JsonArray) {
                            val first = v.firstOrNull()
                            if (first is kotlinx.serialization.json.JsonObject) {
                                val msg = first["message"]?.let { m -> (m as? kotlinx.serialization.json.JsonObject)?.get("content") }
                                val t = msg ?: first["text"] ?: first["content"]
                                if (t is kotlinx.serialization.json.JsonPrimitive && t.isString) return t.content
                            }
                        }

                        if (v is kotlinx.serialization.json.JsonPrimitive && v.isString) return v.content
                        if (v is kotlinx.serialization.json.JsonObject) {
                            val nested = findInObject(v)
                            if (!nested.isNullOrBlank()) return nested
                        }
                        if (v is kotlinx.serialization.json.JsonArray) {
                            val fromArr = v.mapNotNull { item ->
                                if (item is kotlinx.serialization.json.JsonPrimitive) {
                                    if (item.isString) item.content else null
                                } else if (item is kotlinx.serialization.json.JsonObject) {
                                    findInObject(item)
                                } else null
                            }.firstOrNull { it.isNotBlank() }
                            if (!fromArr.isNullOrBlank()) return fromArr
                        }
                    }
                }
                return null
            }

            if (elem is kotlinx.serialization.json.JsonObject) {
                val byKey = findInObject(elem)
                if (!byKey.isNullOrBlank()) return byKey
            }
        } catch (_: Exception) {
            // ignore
        }

        // Fallback: return raw
        return raw.ifBlank { null }
    }

    private fun trimConversationHistory() {
        // Keep all system messages, but limit non-system messages (user/assistant) to the last maxHistoryItems
        val systemMessages = conversationHistory.filter { it.role == "system" }
        val nonSystem = conversationHistory.filter { it.role != "system" }
        val trimmedNonSystem = if (nonSystem.size > maxHistoryItems) nonSystem.takeLast(maxHistoryItems) else nonSystem
        conversationHistory.clear()
        conversationHistory.addAll(systemMessages)
        conversationHistory.addAll(trimmedNonSystem)
    }

    override fun sendMessage(text: String) {
        // append user message locally for UI
        _messages.value = _messages.value + Message(text, true, null, null)

        viewModelScope.launch {
            try {
                val canUseAiTitles = hasApiKey()
                val isFirstUserMessage = conversationHistory.count { it.role == "user" } == 0

                // Get anonymized context from provider
                val context = chatContextProvider.getBehaviorContext()

                // persona/system instruction
                val systemInstruction = "You are a privacy-first digital wellbeing assistant. Only use the anonymized context provided. Return helpful, actionable recommendations. Be concise and prefer bullet points when giving steps."

                // Ensure system instruction and context are present at the start of the conversation history
                if (conversationHistory.none { it.role == "system" && it.content.contains("privacy-first") }) {
                    conversationHistory.add(ChatMessage(role = "system", content = systemInstruction))
                }

                // Add or update a dedicated system context message so LLM always receives up-to-date user behavioral context
                // Replace existing 'context' system message if present
                val contextIndex = conversationHistory.indexOfFirst { it.role == "system" && it.content.startsWith("CONTEXT:") }
                val contextMessage = ChatMessage(role = "system", content = "CONTEXT:\n$context")
                if (contextIndex >= 0) {
                    conversationHistory[contextIndex] = contextMessage
                } else {
                    // add after initial system instruction
                    val insertPos = conversationHistory.indexOfFirst { it.role == "system" } + 1
                    val pos = if (insertPos > 0) insertPos else 0
                    conversationHistory.add(pos, contextMessage)
                }

                // Append the user message to the conversation history
                val userMsg = ChatMessage(role = "user", content = text)
                conversationHistory.add(userMsg)

                // persist user message
                try {
                    conversationId?.let { chatRepository.saveMessage(it, "user", text, System.currentTimeMillis()) }
                    // If conversation title missing, set a default title derived from this user message when AI titles unavailable
                    try {
                        conversationId?.let { id ->
                            val existingTitle = loadStoredTitle(id)
                            if (existingTitle.isNullOrBlank() && (!canUseAiTitles || !isFirstUserMessage)) {
                                applyFallbackTitle(id, text)
                            }
                        }
                    } catch (_: Exception) {}
                } catch (_: Exception) {}

                // Refresh conversation summaries after messages persisted
                try { refreshConversations() } catch (_: Exception) {}

                // Trim history to keep payload small
                trimConversationHistory()

                // Create prompt using full conversation history to keep context across turns
                val prompt = LlmPrompt(
                    system = systemInstruction,
                    user = text,
                    messages = conversationHistory.toList(),
                    metadata = mapOf("model" to model, "provider" to activeProvider)
                )

                // Lookup provider-specific client
                val client = llmProvider.getClient(activeProvider)

                val resp: LlmResponse = client.sendPrompt(prompt)

                val botResponseRaw = resp.structuredJson ?: resp.rawText
                val assistantText = resp.error?.let { "Error from LLM: $it" } ?: extractAssistantText(botResponseRaw) ?: "(No response from assistant)"
                val (thought, finalAnswer) = splitThought(assistantText)
                val visibleAnswer = finalAnswer.ifBlank { "(No response from assistant)" }

                // Append assistant to conversation history and UI messages
                val assistantMsg = ChatMessage(role = "assistant", content = visibleAnswer)
                conversationHistory.add(assistantMsg)

                // persist assistant message
                try {
                    conversationId?.let { chatRepository.saveMessage(it, "assistant", visibleAnswer, System.currentTimeMillis()) }
                } catch (_: Exception) {}

                // Optionally generate a better title using the LLM (non-blocking, best-effort)
                try {
                    conversationId?.let { id ->
                        val existingTitle = loadStoredTitle(id)
                        if (existingTitle.isNullOrBlank()) {
                            val shouldAttemptAi = canUseAiTitles && awaitingInitialAiTitle.getOrElse(id) { true }
                            val generated = if (shouldAttemptAi) tryGenerateAiTitle(id, client) else false
                            awaitingInitialAiTitle[id] = false
                            if (!generated) {
                                applyFallbackTitle(id, text)
                            } else {
                                refreshConversations()
                            }
                        } else {
                            _currentConversationTitle.value = existingTitle
                        }
                    }
                } catch (_: Exception) {}

                // Refresh conversation summaries after assistant message
                try { refreshConversations() } catch (_: Exception) {}

                // Trim again to enforce cap
                trimConversationHistory()

                _messages.value = _messages.value + Message(visibleAnswer, false, model, thought)
            } catch (e: Exception) {
                _messages.value = _messages.value + Message("Error: ${e.message}", false, model, null)
            }
        }
    }

    private fun splitThought(raw: String): Pair<String?, String> {
        val matches = Regex("<think>(.*?)</think>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .findAll(raw)
            .map { it.groupValues[1].trim() }
            .filter { it.isNotBlank() }
            .toList()
        val thought = matches.joinToString("\n\n").ifBlank { null }
        val cleaned = raw.replace(Regex("<think>.*?</think>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "").trim()
        return thought to cleaned
    }

    override fun setProvider(provider: String) {
        activeProvider = provider
        // persist for current conversation if available
        viewModelScope.launch {
            try {
                conversationId?.let { preferencesRepository.setConversationProvider(it, provider) }
            } catch (_: Exception) {}
        }
    }

    override fun setModel(model: String) {
        // update the model used in prompt metadata
        this.model = model
        // persist selection for current conversation
        viewModelScope.launch {
            try {
                conversationId?.let { preferencesRepository.setConversationModel(it, model) }
            } catch (_: Exception) {}
        }
    }

    override fun refreshConversations() {
        viewModelScope.launch {
            try {
                val sums = chatRepository.getConversationSummaries()
                // map and include persisted title from preferencesRepository
                val mapped = sums.map { s ->
                    var title: String? = null
                    try {
                        title = preferencesRepository.getConversationTitle(s.conversation_id).first()
                    } catch (_: Exception) {}
                    ConversationSummary(s.conversation_id, s.lastMessage, s.lastTimestamp, title)
                }
                _conversations.value = mapped
            } catch (_: Exception) {}
        }
    }

    override fun renameConversation(conversationId: String, newTitle: String) {
        viewModelScope.launch {
            try {
                preferencesRepository.setConversationTitle(conversationId, newTitle)
                if (this@ChatViewModel.conversationId == conversationId) {
                    _currentConversationTitle.value = newTitle
                }
                refreshConversations()
            } catch (_: Exception) {}
        }
    }

    override fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            try {
                chatRepository.deleteConversation(conversationId)
                preferencesRepository.setConversationTitle(conversationId, null)
                preferencesRepository.setConversationProvider(conversationId, null)
                preferencesRepository.setConversationModel(conversationId, null)
                if (this@ChatViewModel.conversationId == conversationId) {
                    // reset to a fresh conversation
                    val newId = chatRepository.newConversationId()
                    this@ChatViewModel.conversationId = newId
                    conversationHistory.clear()
                    _messages.value = emptyList()
                    try { preferencesRepository.setLastConversationId(newId) } catch (_: Exception) {}
                    _currentConversationTitle.value = preferencesRepository.getConversationTitle(newId).first()
                }
                refreshConversations()
            } catch (_: Exception) {}
        }
    }

    override fun newConversation() {
        viewModelScope.launch {
            val newId = chatRepository.newConversationId()
            conversationId = newId
            preferencesRepository.setLastConversationId(newId)
            conversationHistory.clear()
            _messages.value = emptyList()
            // Load stored provider/model for this new conversation if set, otherwise leave defaults
            try {
                val prov = preferencesRepository.getConversationProvider(newId).first()
                val mdl = preferencesRepository.getConversationModel(newId).first()
                if (!prov.isNullOrBlank()) activeProvider = prov
                if (!mdl.isNullOrBlank()) model = mdl
            } catch (_: Exception) {}

            _currentConversationTitle.value = preferencesRepository.getConversationTitle(newId).first()
            awaitingInitialAiTitle[newId] = hasApiKey()
        }
    }

    override fun selectConversation(conversationId: String) {
        viewModelScope.launch {
            try {
                val saved = chatRepository.getConversation(conversationId)
                conversationHistory.clear()
                _messages.value = emptyList()
                saved.forEach { e ->
                    conversationHistory.add(ChatMessage(role = e.role, content = e.content))
                    _messages.value = _messages.value + Message(e.content, e.role == "user", null, null)
                }
                this@ChatViewModel.conversationId = conversationId
                preferencesRepository.setLastConversationId(conversationId)

                // load provider/model for the selected conversation
                try {
                    val prov = preferencesRepository.getConversationProvider(conversationId).first()
                    val mdl = preferencesRepository.getConversationModel(conversationId).first()
                    if (!prov.isNullOrBlank()) activeProvider = prov
                    if (!mdl.isNullOrBlank()) model = mdl
                } catch (_: Exception) {}

                _currentConversationTitle.value = preferencesRepository.getConversationTitle(conversationId).first()
            } catch (_: Exception) {}
        }
    }
}
