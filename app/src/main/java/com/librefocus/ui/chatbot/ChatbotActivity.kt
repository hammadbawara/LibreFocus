package com.librefocus.ui.chatbot

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.librefocus.ui.theme.LibreFocusTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.librefocus.data.IApiKeyProvider
import com.librefocus.ai.llm.ProviderModelFetcher
import com.librefocus.ai.llm.GROQ_SUPPORTED_MODELS
import com.librefocus.ai.llm.GEMINI_SUPPORTED_MODELS
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.serialization.json.Json as KotlinxJson
import kotlinx.coroutines.flow.first
import java.io.InputStreamReader
import com.librefocus.data.repository.PreferencesRepository
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

class ChatbotActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val apiKeyProvider: IApiKeyProvider = org.koin.java.KoinJavaComponent.get(IApiKeyProvider::class.java)
        setContent {
            LibreFocusTheme {
                ChatbotScreen(navController = rememberNavController(), viewModel = viewModel, apiKeyProvider = apiKeyProvider)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen(
    navController: NavController,
    viewModel: IChatViewModel = koinViewModel<ChatViewModel>(),
    apiKeyProvider: IApiKeyProvider
) {
    val messages by viewModel.messages.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val currentTitle by viewModel.currentConversationTitle.collectAsState()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var activeProvider by remember { mutableStateOf("groq") }
    var providerError by remember { mutableStateOf<String?>(null) }

    val providerOptions = remember { listOf("groq", "gemini") }

    val context = LocalContext.current
    val initialProviderModels = runCatching {
        val stream = context.assets.open("provider_models.json")
        val text = InputStreamReader(stream).use { it.readText() }
        KotlinxJson.decodeFromString<Map<String, List<String>>>(text)
    }.getOrElse { emptyMap() }

    var providerModelsMap by remember {
        mutableStateOf(
            initialProviderModels.filterKeys { it in providerOptions }.mapValues { (provider, models) ->
                when (provider) {
                    "groq" -> models.filter { GROQ_SUPPORTED_MODELS.contains(it) }
                    "gemini" -> models.filter { GEMINI_SUPPORTED_MODELS.contains(it) }
                    else -> models
                }
            }
        )
    }

    val currentModels = providerModelsMap[activeProvider].orEmpty()
    var selectedModel by remember(activeProvider, providerModelsMap) { mutableStateOf(currentModels.firstOrNull() ?: "") }
    val activeModelLabel = remember(activeProvider, selectedModel) {
        val providerName = activeProvider.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        "$providerName • $selectedModel"
    }

    val modelFetcher = remember { ProviderModelFetcher(apiKeyProvider) }

    LaunchedEffect(activeProvider) {
        try {
            val key = apiKeyProvider.getKey(activeProvider)
            if (!key.isNullOrBlank()) {
                val discovered = modelFetcher.fetchModels(activeProvider)
                if (discovered.isNotEmpty()) {
                    providerModelsMap = providerModelsMap + (activeProvider to discovered)
                    selectedModel = discovered.firstOrNull() ?: selectedModel
                }
            }
        } catch (_: Exception) {}
    }

    val prefsRepo: PreferencesRepository = org.koin.java.KoinJavaComponent.get(PreferencesRepository::class.java)
    val lastConversationId by prefsRepo.lastConversationId.collectAsState(initial = null)

    LaunchedEffect(lastConversationId) {
        try {
            val id = lastConversationId
            if (!id.isNullOrBlank()) {
                val prov = prefsRepo.getConversationProvider(id).first()
                val mdl = prefsRepo.getConversationModel(id).first()
                if (!prov.isNullOrBlank() && providerOptions.contains(prov)) {
                    activeProvider = prov
                }
                if (!mdl.isNullOrBlank()) {
                    selectedModel = mdl
                }
            }
        } catch (_: Exception) {}
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "AI Settings", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Provider", style = MaterialTheme.typography.bodySmall)
                    var provExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = provExpanded, onExpandedChange = { provExpanded = !provExpanded }) {
                        OutlinedTextField(
                            value = activeProvider,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = provExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = provExpanded, onDismissRequest = { provExpanded = false }) {
                            providerOptions.forEach { p ->
                                DropdownMenuItem(text = { Text(p) }, onClick = {
                                    activeProvider = p
                                    provExpanded = false
                                    viewModel.setProvider(p)
                                    val defaults = providerModelsMap[p].orEmpty()
                                    selectedModel = defaults.firstOrNull() ?: ""
                                    viewModel.setModel(selectedModel)
                                })
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val models = providerModelsMap[activeProvider].orEmpty()
                    Text("Model", style = MaterialTheme.typography.bodySmall)
                    var modelExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = modelExpanded, onExpandedChange = { modelExpanded = !modelExpanded }) {
                        OutlinedTextField(
                            value = selectedModel,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = modelExpanded, onDismissRequest = { modelExpanded = false }) {
                            models.forEach { m ->
                                DropdownMenuItem(text = { Text(m) }, onClick = {
                                    selectedModel = m
                                    viewModel.setModel(m)
                                    modelExpanded = false
                                })
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("API Key", style = MaterialTheme.typography.bodySmall)
                    var editingKey by remember { mutableStateOf(false) }
                    val savedKey = apiKeyProvider.getKey(activeProvider)
                    if (!editingKey && !savedKey.isNullOrBlank()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Saved (hidden)")
                            Button(onClick = { editingKey = true }) { Text("Change") }
                        }
                        providerError = null
                    } else {
                        var keyText by remember { mutableStateOf(savedKey ?: "") }
                        OutlinedTextField(value = keyText, onValueChange = { keyText = it }, placeholder = { Text("sk-... or key-...") }, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                            Button(onClick = {
                                apiKeyProvider.saveKey(activeProvider, keyText)
                                editingKey = false
                                providerError = null
                                scope.launch {
                                    try {
                                        val discovered = modelFetcher.fetchModels(activeProvider)
                                        if (discovered.isNotEmpty()) {
                                            providerModelsMap = providerModelsMap + (activeProvider to discovered)
                                            selectedModel = discovered.firstOrNull() ?: selectedModel
                                            viewModel.setModel(selectedModel)
                                        }
                                    } catch (_: Exception) {}
                                }
                            }) { Text("Save Key") }
                            Button(onClick = { apiKeyProvider.clearKey(activeProvider); keyText = ""; editingKey = false; providerError = "API key required for ${activeProvider.uppercase()}" }) { Text("Clear Key") }
                        }
                        providerError = providerError ?: "API key required for ${activeProvider.uppercase()}"
                    }

                    providerError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    var renameDialogVisible by remember { mutableStateOf(false) }
                    var renameTargetId by remember { mutableStateOf("") }
                    var renameText by remember { mutableStateOf("") }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Conversations", style = MaterialTheme.typography.titleSmall)
                        Button(onClick = { viewModel.newConversation() }) { Text("New") }
                    }

                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                        items(conversations) { c ->
                            val title = c.title ?: c.lastMessage?.takeIf { it.isNotBlank() }?.let { if (it.length > 50) it.take(47) + "..." else it } ?: "New Chat"
                            Row(modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectConversation(c.id); scope.launch { drawerState.close() } }
                                .padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        c.lastTimestamp?.let { java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it)) } ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                var itemExpanded by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(onClick = { itemExpanded = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
                                    DropdownMenu(expanded = itemExpanded, onDismissRequest = { itemExpanded = false }) {
                                        DropdownMenuItem(text = { Text("Rename") }, onClick = {
                                            itemExpanded = false
                                            renameTargetId = c.id
                                            renameText = c.title ?: c.lastMessage ?: ""
                                            renameDialogVisible = true
                                        })
                                        DropdownMenuItem(text = { Text("Delete") }, onClick = { itemExpanded = false; viewModel.deleteConversation(c.id) })
                                    }
                                }
                            }
                        }
                    }

                    if (renameDialogVisible) {
                        AlertDialog(
                            onDismissRequest = { renameDialogVisible = false },
                            title = { Text("Rename conversation") },
                            text = {
                                Column {
                                    OutlinedTextField(value = renameText, onValueChange = { renameText = it }, modifier = Modifier.fillMaxWidth())
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    if (renameTargetId.isNotBlank()) {
                                        viewModel.renameConversation(renameTargetId, renameText)
                                        scope.launch {
                                            kotlinx.coroutines.delay(150)
                                            viewModel.refreshConversations()
                                        }
                                    }
                                    renameDialogVisible = false
                                    renameTargetId = ""
                                }) { Text("Save") }
                            },
                            dismissButton = {
                                TextButton(onClick = { renameDialogVisible = false }) { Text("Cancel") }
                            }
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                (currentTitle?.takeIf { it.isNotBlank() } ?: "AI Chatbot"),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            // Removed model subtitle to declutter header
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                    },
                    actions = { IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Filled.MoreVert, contentDescription = "Open menu") } }
                )
            },
            bottomBar = {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    providerError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp))
                    }
                    MessageInput(
                        onSendMessage = { viewModel.sendMessage(it) },
                        selectedModel = selectedModel,
                        onModelSelected = { m -> selectedModel = m; viewModel.setModel(m) }
                    )
                    Text(
                        text = "AI responses may be inaccurate. Verify important information.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 12.dp)) {
                if (messages.isEmpty()) {
                    val suggestions = listOf(
                        "How can I reduce my screen time?",
                        "Give me focus techniques",
                        "What limits are active?"
                    ).shuffled().take(3)

                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Ask AI anything?", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                suggestions.forEach { s -> AssistChip(onClick = { viewModel.sendMessage(s) }, label = { Text(s) }, modifier = Modifier.height(36.dp)) }
                            }
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), reverseLayout = true, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(messages.reversed()) { msg -> MessageBubble(message = msg, assistantLabel = msg.model ?: selectedModel) }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message, assistantLabel: String) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val isUser = message.isFromUser
    val displayText = if (isUser) message.text else formatAssistantText(message.text)
    val annotated = if (isUser) AnnotatedString(displayText) else renderMarkdown(displayText)
    val thoughtText = message.thought?.takeIf { it.isNotBlank() }
    var showThought by remember(message.thought) { mutableStateOf(false) }
    val bubbleShape = if (isUser) {
        RoundedCornerShape(topStart = 20.dp, topEnd = 6.dp, bottomEnd = 20.dp, bottomStart = 20.dp)
    } else {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 6.dp)
    }
    val userColors = MaterialTheme.colorScheme.primaryContainer
    val assistantBackground = MaterialTheme.colorScheme.surfaceVariant
    val bubbleBrush = if (isUser) {
        Brush.linearGradient(listOf(userColors, userColors))
    } else {
        Brush.linearGradient(listOf(assistantBackground, assistantBackground))
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val label = if (isUser) "You" else assistantLabel.ifBlank { "Assistant" }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            ParticipantAvatar(isUser = false)
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .clip(bubbleShape)
                    .background(bubbleBrush)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = annotated,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                    if (!thoughtText.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Thought process",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    IconButton(onClick = { showThought = !showThought }) {
                                        Icon(
                                            imageVector = if (showThought) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                            contentDescription = if (showThought) "Hide thought" else "Show thought"
                                        )
                                    }
                                }
                                AnimatedVisibility(visible = showThought) {
                                    Text(
                                        text = thoughtText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    if (!isUser && message.text.isNotBlank()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton(onClick = {
                                clipboardManager.setText(AnnotatedString(displayText))
                                Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy response", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
            // Removed copy chip label UI
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            ParticipantAvatar(isUser = true)
        }
    }
}

@Composable
private fun ParticipantAvatar(isUser: Boolean) {
    Surface(
        shape = CircleShape,
        color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
        tonalElevation = 6.dp
    ) {
        Icon(
            imageVector = if (isUser) Icons.Filled.Person else Icons.Filled.Lightbulb,
            contentDescription = if (isUser) "User" else "Assistant",
            modifier = Modifier.size(24.dp).padding(6.dp),
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageInput(
    onSendMessage: (String) -> Unit,
    selectedModel: String,
    onModelSelected: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = "Model in use: $selectedModel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerText ->
                        Box {
                            if (text.isBlank()) {
                                Text(
                                    text = "Ready to focus? Ask anything...",
                                    style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                            innerText()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 84.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (text.isNotEmpty()) {
                        IconButton(onClick = { text = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                        }
                    }
                    Button(
                        onClick = {
                            val trimmed = text.trim()
                            if (trimmed.isNotEmpty() && !isSending) {
                                isSending = true
                                onSendMessage(trimmed)
                                text = ""
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(500)
                                    isSending = false
                                }
                            }
                        },
                        enabled = text.trim().isNotEmpty() && !isSending,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}

private fun formatAssistantText(raw: String): String {
    return raw
        .trim()
        .split('\n')
        .map { line ->
            line
                .trimEnd()
                .replace(Regex("  +"), " ")
                .replace(Regex("\\s+\\*\\*"), "**")
                .replace(Regex("\\*\\*\\s+"), "**")
                .replace(Regex("\\s+([.,!?])"), "$1")
        }
        .joinToString("\n")
}

@Composable
private fun renderMarkdown(text: String): AnnotatedString {
    val lines = text.split('\n')
    val builder = AnnotatedString.Builder()
    val h1 = MaterialTheme.typography.titleLarge.fontSize
    val h2 = MaterialTheme.typography.titleMedium.fontSize
    val h3 = MaterialTheme.typography.titleSmall.fontSize

    var inCodeBlock = false

    lines.forEachIndexed { index, rawLine ->
        val line = rawLine.trimEnd()
        if (line.startsWith("```") ) {
            inCodeBlock = !inCodeBlock
            // Do not render the fence itself
        } else if (inCodeBlock) {
            builder.pushStyle(SpanStyle(fontFamily = FontFamily.Monospace))
            builder.append(line)
            builder.pop()
        } else when {
            line.startsWith("### ") -> appendStyledLine(builder, line.removePrefix("### "), h3, FontWeight.SemiBold)
            line.startsWith("## ") -> appendStyledLine(builder, line.removePrefix("## "), h2, FontWeight.SemiBold)
            line.startsWith("# ") -> appendStyledLine(builder, line.removePrefix("# "), h1, FontWeight.Bold)
            line.startsWith("- ") || line.startsWith("* ") -> {
                builder.append("• ")
                appendInlineStyles(builder, line.drop(2), SpanStyle(fontSize = MaterialTheme.typography.bodyMedium.fontSize))
            }
            line.firstOrNull()?.isDigit() == true && line.dropWhile { it.isDigit() || it == '.' || it == ' ' }.let { it.length < line.length } -> {
                // simple numbered list detection like "1. Item"
                val content = line.dropWhile { it.isDigit() || it == '.' || it == ' ' }
                builder.append("• ")
                appendInlineStyles(builder, content, SpanStyle(fontSize = MaterialTheme.typography.bodyMedium.fontSize))
            }
            line.isBlank() -> builder.append("")
            else -> appendInlineStyles(builder, line, SpanStyle(fontSize = MaterialTheme.typography.bodyMedium.fontSize))
        }
        if (index != lines.lastIndex) builder.append('\n')
    }
    return builder.toAnnotatedString()
}

private fun appendStyledLine(builder: AnnotatedString.Builder, text: String, size: androidx.compose.ui.unit.TextUnit, weight: FontWeight) {
    appendInlineStyles(builder, text, SpanStyle(fontSize = size, fontWeight = weight))
}

private fun appendInlineStyles(builder: AnnotatedString.Builder, text: String, baseStyle: SpanStyle) {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("***", i) -> {
                val end = text.indexOf("***", i + 3)
                if (end > i) {
                    val content = text.substring(i + 3, end)
                    builder.pushStyle(baseStyle.merge(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)))
                    builder.append(content)
                    builder.pop()
                    i = end + 3
                    continue
                }
            }
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end > i) {
                    val content = text.substring(i + 2, end)
                    builder.pushStyle(baseStyle.merge(SpanStyle(fontWeight = FontWeight.SemiBold)))
                    builder.append(content)
                    builder.pop()
                    i = end + 2
                    continue
                }
            }
            text.startsWith("*", i) -> {
                val end = text.indexOf("*", i + 1)
                if (end > i) {
                    val content = text.substring(i + 1, end)
                    builder.pushStyle(baseStyle.merge(SpanStyle(fontStyle = FontStyle.Italic)))
                    builder.append(content)
                    builder.pop()
                    i = end + 1
                    continue
                }
            }
        }
        builder.pushStyle(baseStyle)
        builder.append(text[i])
        builder.pop()
        i++
    }
}
