@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.chatbotapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.chatbotapp.ui.theme.ChatbotAppTheme
import com.example.chatbotapp.screens.*
import com.example.chatbotapp.data.ChatSession
import com.example.chatbotapp.data.ChatMessage
import com.example.chatbotapp.auth.AuthService
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Close
import android.util.Log
import com.example.chatbotapp.data.updateChatTitle
import kotlinx.coroutines.launch
import com.example.chatbotapp.api.OpenAIService
import android.app.Activity
import android.content.Intent
import android.content.ActivityNotFoundException
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest
import android.net.Uri
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            var isDark by rememberSaveable { mutableStateOf(false) }
            val authService = remember { AuthService() }

            ChatbotAppTheme(darkTheme = isDark) {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "welcome") {

                    composable("welcome") {
                        WelcomeScreen(navController)
                    }

                    composable("login") {
                        LoginScreen(navController)
                    }

                    composable("signup") {
                        SignUpScreen(navController)
                    }

                    composable("about") {
                        AboutScreen(onBack = { navController.popBackStack() })
                    }

                    composable("privacySecurity") {
                        PrivacySecurityScreen(onBack = { navController.popBackStack() })
                    }

                    composable("helpSupport") {
                        HelpSupportScreen(onBack = { navController.popBackStack() })
                    }

                    composable("editProfile") {
                        EditProfileScreen(navController)
                    }

                    composable(
                        route = "main?selectedTab={selectedTab}",
                        arguments = listOf(
                            navArgument("selectedTab") {
                                type = NavType.IntType
                                defaultValue = 0
                            }
                        )
                    ) { backStackEntry ->
                        val selectedTab = backStackEntry.arguments?.getInt("selectedTab") ?: 0

                        MainScreen(
                            navController = navController,
                            onLogout = {
                                authService.signOut()
                                navController.navigate("welcome") {
                                    popUpTo("main") { inclusive = true }
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            isDark = isDark,
                            onToggleTheme = { isDark = !isDark },
                            initialTab = selectedTab
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    navController: NavController,
    onLogout: () -> Unit,
    initialTab: Int = 0,
    isDark: Boolean,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current
    var isScraping by remember { mutableStateOf(false) }
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val selectedTabArg = navBackStackEntry.value?.arguments?.getInt("selectedTab", initialTab)

    var selectedTab by rememberSaveable { mutableStateOf(selectedTabArg ?: initialTab) }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        }
    }

    val startVoiceInput: () -> Unit = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your message...")
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e("STT", "Speech recognition not supported on this device.", e)
        }
    }

    var showPermissionDeniedMessage by remember { mutableStateOf(false) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startVoiceInput()
        } else {
            showPermissionDeniedMessage = true
        }
    }

    val handleMicClick: () -> Unit = {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startVoiceInput()
            }
            else -> {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    var showChatHistory by remember { mutableStateOf(false) }

    val openAIService = remember { OpenAIService() }
    val scope = rememberCoroutineScope()

    val chatSessions = remember { mutableStateListOf<ChatSession>() }
    var currentChatId by rememberSaveable { mutableStateOf("") }

    var webViewUrl by remember { mutableStateOf<String?>(null) }

    var isInitialLoad by remember { mutableStateOf(true) }

    var browserInstructionMessage by remember { mutableStateOf<String?>(null) }

    val degreeworksInstruction = "🌐 Please sign in to the Morgan Gateway and navigate directly to your Degreeworks page. When ready, tap the 'Scrape' button."

    val launchInAppBrowser: (String) -> Unit = { url ->
        webViewUrl = url
        browserInstructionMessage = degreeworksInstruction
    }

    val closeWebView: () -> Unit = {
        webViewUrl = null
        browserInstructionMessage = null
        isScraping = false
    }

    val handleHtmlScraped: (String) -> Unit = { html ->
        closeWebView()
        isScraping = true

        scope.launch {
            Log.d("Scrape", "Received HTML. Initiating API call.")

            val success = openAIService.scrapeAndSaveDegreeData(html)

            isScraping = false

            val resultMessage = if (success) {
                "✅ Degree audit data successfully scraped and saved! You can now ask questions about your degree progress."
            } else {
                "❌ Degree audit scraping failed. Please ensure you are logged into your DegreeWorks page and try again."
            }

            val currentChat = chatSessions.find { it.id == currentChatId } ?: chatSessions.firstOrNull()
            currentChat?.let { session ->
                session.messages.add(ChatMessage(text = resultMessage, isUser = false, timestamp = System.currentTimeMillis().toString()))
                openAIService.saveChatUpdate(session, session.messages.last())
            }
        }
    }

    LaunchedEffect(Unit) {
        if (isInitialLoad) {
            try {
                val fetchedSessions = openAIService.fetchChatSessions()

                if (fetchedSessions.isNotEmpty()) {
                    chatSessions.addAll(fetchedSessions)
                    currentChatId = chatSessions.first().id
                } else {
                    val newChat = ChatSession(
                        title = "New Chat",
                        messages = mutableListOf(
                            ChatMessage(
                                text = "Hello! I'm your AI assistant. How can I help you today?",
                                isUser = false,
                                timestamp = System.currentTimeMillis().toString()
                            )
                        )
                    )
                    chatSessions.add(newChat)
                    currentChatId = newChat.id
                }
            } catch (e: Exception) {
                Log.e("MainScreen", "Error loading chats: ${e.message}")
                val newChat = ChatSession(
                    title = "New Chat",
                    messages = mutableListOf(
                        ChatMessage(
                            text = "Hello! I'm your AI assistant. How can I help you today?",
                            isUser = false,
                            timestamp = System.currentTimeMillis().toString()
                        )
                    )
                )
                chatSessions.add(newChat)
                currentChatId = newChat.id
            }
            isInitialLoad = false
        }
    }

    val tabs = listOf("Chat", "Curriculum", "Profile")
    val icons = listOf(
        Icons.Filled.Chat,
        Icons.Filled.School,
        Icons.Filled.Person
    )

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = when (selectedTab) {
                                    0 -> "AI Assistant"
                                    1 -> "Curriculum"
                                    2 -> "Profile"
                                    else -> "ChatBot"
                                },
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            if (selectedTab == 0) {
                                OutlinedButton(
                                    onClick = { showChatHistory = true },
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        Icons.Filled.History,
                                        contentDescription = "Chat History",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "History",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }
                    },

                    actions = {
                        IconButton(
                            onClick = onToggleTheme,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = "Toggle theme",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (selectedTab != 2) {
                            IconButton(
                                onClick = onLogout,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Filled.ExitToApp,
                                    contentDescription = "Logout",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                browserInstructionMessage?.let { message ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = "Information",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = message,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(onClick = { closeWebView() }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                icons[index],
                                contentDescription = tab,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                tab,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { inner ->
        if (webViewUrl != null) {
            WebViewScreen(
                url = webViewUrl!!,
                onClose = closeWebView,
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize())
        } else {
            when (selectedTab) {
                0 -> {
                    if (isScraping) {
                        Box(
                            modifier = Modifier
                                .padding(inner)
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(16.dp))
                                Text("Analyzing Degreeworks data...", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    } else {
                        val currentChat = chatSessions.find { it.id == currentChatId } ?: chatSessions.firstOrNull()

                        if (currentChat != null) {
                            ChatContent(
                                modifier = Modifier.padding(inner),
                                chatSession = currentChat,
                                chatSessions = chatSessions,
                                currentChatId = currentChatId,
                                onNewChat = {
                                    val newChat = ChatSession(
                                        title = "New Chat",
                                        messages = mutableListOf(
                                            ChatMessage(
                                                text = "Hello! I'm your AI assistant. How can I help you today?",
                                                isUser = false,
                                                timestamp = System.currentTimeMillis().toString()
                                            )
                                        )
                                    )
                                    chatSessions.add(0, newChat)
                                    currentChatId = newChat.id
                                },
                                onMessageSent = {
                                    currentChat.messages.lastOrNull()?.let { lastMessage ->
                                        scope.launch {
                                            openAIService.saveChatUpdate(currentChat, lastMessage)
                                            chatSessions.remove(currentChat)
                                            chatSessions.add(0, currentChat)
                                        }
                                    }
                                },
                                onChatSelected = { chatId ->
                                    currentChatId = chatId
                                },
                                onChatDeleted = { chatId ->
                                    val chatToDelete = chatSessions.find { it.id == chatId }
                                    chatToDelete?.let { session ->
                                        scope.launch {
                                            try {
                                                openAIService.deleteChat(session.id)
                                                chatSessions.removeAll { it.id == chatId }

                                                if (currentChatId == chatId && chatSessions.isNotEmpty()) {
                                                    currentChatId = chatSessions[0].id
                                                } else if (chatSessions.isEmpty()) {
                                                    val newChat = ChatSession(
                                                        title = "New Chat",
                                                        messages = mutableListOf(
                                                            ChatMessage(
                                                                text = "Hello! I'm your AI assistant. How can I help you today?",
                                                                isUser = false,
                                                                timestamp = System.currentTimeMillis().toString()
                                                            )
                                                        )
                                                    )
                                                    chatSessions.add(newChat)
                                                    currentChatId = newChat.id
                                                }
                                            } catch (e: Exception) {
                                                Log.e("MainScreen", "Failed to delete chat: ${e.message}")
                                            }
                                        }
                                    }
                                }
                            )
                        } else {
                            Box(modifier = Modifier.padding(inner).fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }

                1 -> CurriculumContent(
                    modifier = Modifier.padding(inner),
                    onLaunchBrowser = launchInAppBrowser
                )

                2 -> ProfileContent(
                    modifier = Modifier.padding(inner),
                    onEditProfile = { navController.navigate("editProfile") },
                    onAbout = { navController.navigate("about") },
                    onPrivacySecurity = { navController.navigate("privacySecurity") },
                    onHelpSupport = { navController.navigate("helpSupport") },
                    onLogout = onLogout
                )
            }
        }

        if (showChatHistory) {
            ChatHistoryDrawer(
                chatSessions = chatSessions,
                currentChatId = currentChatId,
                onDismiss = { showChatHistory = false },
                onChatSelected = { chatId ->
                    currentChatId = chatId
                    showChatHistory = false
                },
                onChatDeleted = { chatId ->
                    val chatToDelete = chatSessions.find { it.id == chatId }
                    chatToDelete?.let { session ->
                        scope.launch {
                            try {
                                openAIService.deleteChat(session.id)
                                chatSessions.removeAll { it.id == chatId }

                                if (currentChatId == chatId && chatSessions.isNotEmpty()) {
                                    currentChatId = chatSessions[0].id
                                } else if (chatSessions.isEmpty()) {
                                    val newChat = ChatSession(
                                        title = "New Chat",
                                        messages = mutableListOf(
                                            ChatMessage(
                                                text = "Hello! I'm your AI assistant. How can I help you today?",
                                                isUser = false,
                                                timestamp = System.currentTimeMillis().toString()
                                            )
                                        )
                                    )
                                    chatSessions.add(newChat)
                                    currentChatId = newChat.id
                                }
                            } catch (e: Exception) {
                                Log.e("MainScreen", "Failed to delete chat: ${e.message}")
                            }
                        }
                    }
                }
            )
        }
    }
}
