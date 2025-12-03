// File: app/src/main/java/com/example/chatbotapp/screens/ChatScreen.kt
@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.chatbotapp.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatbotapp.data.ChatMessage
import com.example.chatbotapp.data.ChatSession
import com.example.chatbotapp.data.updateChatTitle
import com.example.chatbotapp.api.OpenAIService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.chatbotapp.R
import android.util.Log

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import java.util.Locale


// Dark theme colors
private val ChatDarkBg = Color(0xFF1E1E2E)
private val ChatCardBg = Color(0xFF2A2A3E)
private val AccentGlow = Color(0xFF3A3A5E)

@Composable
fun ChatContent(
    modifier: Modifier = Modifier,
    chatSession: ChatSession,
    onNewChat: () -> Unit,
    onMessageSent: () -> Unit = {},
    chatSessions: List<ChatSession> = emptyList(),
    currentChatId: String = "",
    onChatSelected: (String) -> Unit = {},
    onChatDeleted: (String) -> Unit = {}
) {
    var userInput by remember { mutableStateOf(TextFieldValue("")) }
    var isTyping by remember { mutableStateOf(false) }
    var showHistoryDrawer by remember { mutableStateOf(false) }
    val openAIService = remember { OpenAIService() }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val suggestedPrompts = remember {
        listOf(
            "📚 What courses should I take next semester?",
            "🗓️ Show me the academic calendar",
            "💡 Give me study tips for finals",
            "🎯 Career advice for CS majors",
            "🏫 Campus facilities and resources",
            "📝 How to register for classes?"
        )
    }

    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            tts?.shutdown()
        }
    }


    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            ChatDarkBg,
            MorganBlue.copy(alpha = 0.4f),
            ChatDarkBg,
            MorganBlue.copy(alpha = 0.2f)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            PremiumTopBar(
                chatSession = chatSession,
                onNewChat = onNewChat,
                onHistoryClick = { showHistoryDrawer = true }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    reverseLayout = false
                ) {
                    if (chatSession.messages.isEmpty()) {
                        item { EpicWelcomeCard() }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "✨ Quick Actions",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MorganOrange
                                    )
                                )
                                Text(
                                    text = "Tap to ask",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = LightText.copy(alpha = 0.6f)
                                    )
                                )
                            }
                        }

                        item {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                suggestedPrompts.chunked(2).forEach { rowPrompts ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        rowPrompts.forEach { prompt ->
                                            SuggestedPromptCard(
                                                text = prompt,
                                                onClick = {
                                                    userInput = TextFieldValue(prompt.substringAfter(" ").trim())
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        if (rowPrompts.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            FeatureHighlights()
                        }
                    }

                    items(
                        items = chatSession.messages,
                        key = { message -> "${message.timestamp}-${message.isUser}-${message.text.take(50).hashCode()}" }
                    ) { message ->
                        DarkMessageBubble(message = message, tts = tts)
                    }

                    if (isTyping) {
                        item {
                            DarkTypingIndicator()
                        }
                    }
                }
            }

            PremiumInputArea(
                userInput = userInput,
                onInputChange = { userInput = it },
                isTyping = isTyping,
                onSendMessage = {
                    if (userInput.text.isNotBlank()) {
                        val query = userInput.text
                        chatSession.messages.add(
                            ChatMessage(
                                text = query,
                                isUser = true,
                                timestamp = System.currentTimeMillis().toString()
                            )
                        )
                        updateChatTitle(chatSession, query)
                        userInput = TextFieldValue("")
                        onMessageSent()
                        isTyping = true

                        coroutineScope.launch {
                            delay(100)
                            listState.animateScrollToItem(chatSession.messages.size)

                            try {
                                val response = openAIService.getChatResponse(query)

                                isTyping = false
                                chatSession.messages.add(
                                    ChatMessage(
                                        text = response,
                                        isUser = false,
                                        timestamp = System.currentTimeMillis().toString()
                                    )
                                )
                                onMessageSent()
                                delay(100)
                                listState.animateScrollToItem(chatSession.messages.size)

                            } catch (e: Exception) {
                                isTyping = false
                                Log.e("ChatContent", "Error getting response: ${e.message}", e)

                                val fallbackResponse = when {
                                    e.message?.contains("API key", ignoreCase = true) == true ->
                                        "⚠️ API key error. Please check your OpenAI configuration."
                                    e.message?.contains("network", ignoreCase = true) == true ->
                                        "🌐 Network error. Please check your internet connection."
                                    else ->
                                        "Sorry, I encountered an error: ${e.message}. Please try again."
                                }

                                chatSession.messages.add(
                                    ChatMessage(
                                        text = fallbackResponse,
                                        isUser = false,
                                        timestamp = System.currentTimeMillis().toString()
                                    )
                                )
                                onMessageSent()
                            }
                        }
                    }
                }
            )
        }

        if (showHistoryDrawer) {
            ChatHistoryDrawer(
                chatSessions = chatSessions,
                currentChatId = currentChatId,
                onDismiss = { showHistoryDrawer = false },
                onChatSelected = { chatId ->
                    onChatSelected(chatId)
                    showHistoryDrawer = false
                },
                onChatDeleted = { chatId ->
                    onChatDeleted(chatId)
                }
            )
        }
    }
}

@Composable
fun PremiumTopBar(
    chatSession: ChatSession,
    onNewChat: () -> Unit,
    onHistoryClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ChatCardBg.copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, MorganOrange.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.chat_logo),
                        contentDescription = "MSU",
                        modifier = Modifier
                            .size(32.dp)
                            .padding(4.dp),
                        tint = Color.Unspecified
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "MSU AI Assistant",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = LightText
                        )
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PulsingDot()
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Online • AI Powered",
                            style = MaterialTheme.typography.bodySmall,
                            color = LightText.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedActionButton(
                    icon = Icons.Filled.History,
                    contentDescription = "History",
                    onClick = onHistoryClick
                )

                AnimatedActionButton(
                    icon = Icons.Filled.Add,
                    contentDescription = "New Chat",
                    onClick = onNewChat,
                    isPrimary = true
                )
            }
        }
    }
}

@Composable
fun AnimatedActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    isPrimary: Boolean = false
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    IconButton(
        onClick = {
            pressed = true
            onClick()
        },
        modifier = Modifier
            .size(40.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(if (isPrimary) MorganOrange else AccentGlow)
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (isPrimary) Color.White else MorganOrange,
            modifier = Modifier.size(20.dp)
        )
    }

    LaunchedEffect(pressed) {
        if (pressed) {
            delay(100)
            pressed = false
        }
    }
}

@Composable
fun PulsingDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .size(8.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(Color(0xFF4CAF50))
    )
}

@Composable
fun EpicWelcomeCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ChatCardBg.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MorganOrange.copy(alpha = 0.2f),
                                Color.Transparent
                            ),
                            radius = 400f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.chat_logo),
                        contentDescription = "Welcome",
                        modifier = Modifier.size(48.dp),
                        tint = Color.Unspecified
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Welcome Bear! 🐻",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = LightText,
                        fontSize = 28.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your AI-powered assistant for all things Morgan State",
                    style = MaterialTheme.typography.bodyLarge,
                    color = LightText.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatChip("🎓", "Courses")
                    StatChip("📅", "Events")
                    StatChip("💡", "Tips")
                }
            }
        }
    }
}

@Composable
fun FeatureHighlights() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "🚀 What I can help with",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = LightText
            )
        )

        FeatureItem("📚", "Course Information", "Prerequisites, schedules, and more")
        FeatureItem("🗓️", "Academic Calendar", "Important dates and deadlines")
        FeatureItem("💡", "Study Resources", "Tips, tools, and guidance")
        FeatureItem("🏛️", "Campus Life", "Facilities, events, and activities")
    }
}

@Composable
fun FeatureItem(emoji: String, title: String, description: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = ChatCardBg.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, AccentGlow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                emoji,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = LightText
                    )
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = LightText.copy(alpha = 0.7f)
                    )
                )
            }
        }
    }
}

@Composable
fun StatChip(emoji: String, label: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = AccentGlow.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MorganOrange.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(emoji, fontSize = 16.sp)
            Text(
                label,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = LightText,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
fun SuggestedPromptCard(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Surface(
        onClick = {
            pressed = true
            onClick()
        },
        modifier = modifier
            .height(90.dp)
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        color = ChatCardBg.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MorganOrange.copy(alpha = 0.2f)),
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = LightText,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp
                )
            )
        }
    }

    LaunchedEffect(pressed) {
        if (pressed) {
            delay(100)
            pressed = false
        }
    }
}

// Replace the DarkMessageBubble function with this corrected version:

@Composable
fun DarkMessageBubble(
    message: ChatMessage,
    tts: TextToSpeech?
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(50)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(400)) +
                slideInVertically(
                    initialOffsetY = { 40 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) +
                scaleIn(
                    initialScale = 0.9f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!message.isUser) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.chat_logo),
                        contentDescription = "AI",
                        modifier = Modifier.size(20.dp),
                        tint = Color.Unspecified
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Surface(
                modifier = Modifier.widthIn(max = 280.dp),
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = if (message.isUser) 20.dp else 4.dp,
                    bottomEnd = if (message.isUser) 4.dp else 20.dp
                ),
                color = if (message.isUser) MorganOrange else ChatCardBg.copy(alpha = 0.9f),
                shadowElevation = 6.dp,
                border = if (!message.isUser) BorderStroke(1.dp, AccentGlow) else null
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        ClickableMessageText(
                            text = message.text,
                            isUser = message.isUser
                        )
                    }

                    if (!message.isUser) {
                        Icon(
                            imageVector = Icons.Filled.VolumeUp,
                            contentDescription = "Read aloud",
                            tint = MorganOrange,
                            modifier = Modifier
                                .size(22.dp)
                                .padding(start = 8.dp)
                                .clickable {
                                    tts?.speak(
                                        message.text,
                                        TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        "AI_MESSAGE_TTS"
                                    )
                                }
                        )
                    }
                }
            }
        }
    }
}

// Move ClickableMessageText OUTSIDE of DarkMessageBubble as a separate function:
@Composable
fun ClickableMessageText(
    text: String,
    isUser: Boolean
) {
    val uriHandler = LocalUriHandler.current

    val urlPattern = Regex("https?://[^\\s]+")
    val matches = urlPattern.findAll(text).toList()

    if (matches.isEmpty()) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = if (isUser) Color.White else LightText,
                lineHeight = 22.sp
            )
        )
    } else {
        val annotatedString = buildAnnotatedString {
            var lastIndex = 0

            matches.forEach { match ->
                append(text.substring(lastIndex, match.range.first))

                val url = match.value
                val displayText = extractDisplayName(url)

                pushStringAnnotation(tag = "URL", annotation = url)
                withStyle(
                    style = SpanStyle(
                        color = if (isUser) Color.White else Color(0xFF4A9EFF),
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.SemiBold
                    )
                ) {
                    append(displayText)
                }
                pop()

                lastIndex = match.range.last + 1
            }

            if (lastIndex < text.length) {
                append(text.substring(lastIndex))
            }
        }

        ClickableText(
            text = annotatedString,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = if (isUser) Color.White else LightText,
                lineHeight = 22.sp
            ),
            onClick = { offset ->
                annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        try {
                            uriHandler.openUri(annotation.item)
                        } catch (e: Exception) {
                            Log.e("ClickableText", "Failed to open URL: ${e.message}")
                        }
                    }
            }
        )
    }
}

// Keep extractDisplayName as a top-level function (outside composables):
fun extractDisplayName(url: String): String {
    return when {
        url.contains("morgan.edu/calendar") -> "📅 Morgan State Calendar"
        url.contains("morgan.edu/registrar") -> "📝 Registrar"
        url.contains("morgan.edu/admissions") -> "🎓 Admissions"
        url.contains("morgan.edu") -> "🏛️ Morgan State Website"
        url.contains("degreeworks") -> "🎓 DegreeWorks"
        else -> {
            val domain = url.substringAfter("://").substringBefore("/").substringAfter("www.")
            "🔗 $domain"
        }
    }
}

@Composable
fun DarkTypingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.chat_logo),
                contentDescription = "AI",
                modifier = Modifier.size(20.dp),
                tint = Color.Unspecified
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ChatCardBg.copy(alpha = 0.9f),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    TypingDot(index)
                }
            }
        }
    }
}

@Composable
fun TypingDot(index: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = index * 100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot$index"
    )

    Box(
        modifier = Modifier
            .size(8.dp)
            .offset(y = offset.dp)
            .clip(CircleShape)
            .background(MorganOrange)
    )
}

@Composable
fun PremiumInputArea(
    userInput: TextFieldValue,
    onInputChange: (TextFieldValue) -> Unit,
    isTyping: Boolean,
    onSendMessage: () -> Unit
) {
    val context = LocalContext.current
    var statusMessage by remember { mutableStateOf("") }

    // Use Activity Result API for voice input - more reliable than SpeechRecognizer
    val voiceInputLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
            if (!spokenText.isNullOrEmpty()) {
                Log.d("VoiceInput", "✅ Recognized: $spokenText")
                onInputChange(TextFieldValue(spokenText))
                statusMessage = "Got it!"

                kotlinx.coroutines.MainScope().launch {
                    delay(2000)
                    statusMessage = ""
                }
            } else {
                statusMessage = "No speech detected"
                kotlinx.coroutines.MainScope().launch {
                    delay(2000)
                    statusMessage = ""
                }
            }
        } else {
            Log.d("VoiceInput", "Voice input cancelled or failed")
            statusMessage = "Voice input cancelled"
            kotlinx.coroutines.MainScope().launch {
                delay(2000)
                statusMessage = ""
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Launch voice input activity
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now... 🎤")
            }
            try {
                voiceInputLauncher.launch(intent)
            } catch (e: Exception) {
                Log.e("VoiceInput", "Error launching voice input: ${e.message}")
                statusMessage = "Voice input not available"
            }
        } else {
            statusMessage = "Microphone permission denied"
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ChatCardBg.copy(alpha = 0.95f),
        shadowElevation = 16.dp
    ) {
        Column {
            // Status message banner
            AnimatedVisibility(
                visible = statusMessage.isNotEmpty(),
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MorganOrange.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = statusMessage,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = LightText,
                            fontWeight = FontWeight.Medium
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 🎤 Mic Button - launches Google's voice input dialog
                IconButton(
                    onClick = {
                        if (context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
                            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        } else {
                            // Launch voice input activity
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now... 🎤")
                            }
                            try {
                                voiceInputLauncher.launch(intent)
                            } catch (e: Exception) {
                                Log.e("VoiceInput", "Error: ${e.message}")
                                statusMessage = "Voice input not available on this device"
                                kotlinx.coroutines.MainScope().launch {
                                    delay(3000)
                                    statusMessage = ""
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AccentGlow)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Voice input",
                        tint = MorganOrange
                    )
                }

                // 📝 Input Field
                OutlinedTextField(
                    value = userInput,
                    onValueChange = onInputChange,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp, max = 120.dp),
                    placeholder = {
                        Text(
                            "Ask me anything...",
                            color = LightText.copy(alpha = 0.5f)
                        )
                    },
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MorganOrange,
                        unfocusedBorderColor = AccentGlow,
                        cursorColor = MorganOrange,
                        focusedContainerColor = ChatDarkBg.copy(alpha = 0.5f),
                        unfocusedContainerColor = ChatDarkBg.copy(alpha = 0.3f),
                        focusedTextColor = LightText,
                        unfocusedTextColor = LightText
                    ),
                    enabled = !isTyping
                )

                // Send Button
                FloatingActionButton(
                    onClick = onSendMessage,
                    modifier = Modifier.size(56.dp),
                    containerColor = if (userInput.text.isBlank() || isTyping)
                        AccentGlow
                    else MorganOrange,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = if (userInput.text.isBlank()) 0.dp else 8.dp
                    )
                ) {
                    Icon(
                        Icons.Filled.Send,
                        contentDescription = "Send",
                        tint = if (userInput.text.isBlank() || isTyping)
                            LightText.copy(alpha = 0.5f)
                        else Color.White
                    )
                }
            }
        }
    }
}
@Composable
fun ChatHistoryDrawer(
    chatSessions: List<ChatSession> = emptyList(),
    currentChatId: String = "",
    onDismiss: () -> Unit,
    onChatSelected: (String) -> Unit,
    onChatDeleted: (String) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onDismiss)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight()
                .align(Alignment.CenterStart)
                .clickable(enabled = false) { },
            color = ChatDarkBg,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "📜 Chat History",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = LightText
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, "Close", tint = LightText)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (chatSessions.isEmpty()) {
                    Text(
                        "No previous chats",
                        color = LightText.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = chatSessions,
                            key = { it.id }
                        ) { session ->
                            ChatHistoryItem(
                                session = session,
                                isSelected = session.id == currentChatId,
                                onSelect = {
                                    onChatSelected(session.id)
                                    onDismiss()
                                },
                                onDelete = {
                                    onChatDeleted(session.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatHistoryItem(
    session: ChatSession,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MorganOrange.copy(alpha = 0.3f) else ChatCardBg.copy(alpha = 0.6f),
        border = if (isSelected) BorderStroke(2.dp, MorganOrange) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = LightText
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (session.messages.isNotEmpty()) {
                    Text(
                        text = "${session.messages.size} messages",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = LightText.copy(alpha = 0.6f)
                        )
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = Color(0xFFFF6B6B),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
