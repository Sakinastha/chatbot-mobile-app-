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
import com.example.chatbotapp.components.ChatHistoryModal
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
import androidx.browser.customtabs.CustomTabsIntent
import android.net.Uri
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ============================================
        // FIREBASE JSON UPLOAD - RUN ONCE THEN COMMENT OUT
        // ============================================
        //uploadJSONFilesToFirebase()
        // ============================================

        enableEdgeToEdge()
        setContent {
            var isDark by rememberSaveable { mutableStateOf(false) }
            val authService = remember { AuthService() }

            ChatbotAppTheme(darkTheme = isDark) {
                val navController = rememberNavController()

                // Determine starting destination based on auth state
                val startDestination = if (authService.isUserSignedIn) "main" else "login"

                NavHost(navController = navController, startDestination = "main?selectedTab=0") {

                    composable("login") {
                        LoginScreen(navController)
                    }

                    composable("signup") {
                        SignUpScreen(navController)
                    }

                    // ✅ Edit Profile Screen
                    composable("editProfile") {
                        EditProfileScreen(navController = navController)
                    }

                    // ✅ Main route using path argument (stable & type-safe)
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
                                navController.navigate("login") {
                                    popUpTo("main/{selectedTab}") { inclusive = true }
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


    private fun uploadJSONFilesToFirebase() {
        try {
            Log.d("MainActivity", "Starting Firebase upload...")
            // Assuming FirebaseDataUploader and its methods exist
            // val uploader = FirebaseDataUploader(this)
            // uploader.uploadWithNestedStructure()
            Log.d("MainActivity", "Firebase upload initiated. Check Logcat for progress.")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error uploading to Firebase", e)
        }
    }
}

@Composable
fun MainScreen(
    navController: NavController,
    onLogout: () -> Unit,
    initialTab: Int = 0,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    voiceInput: String? = null,
    onVoiceInputComplete: () -> Unit = {},
    onMicClicked: () -> Unit = {}
) {
    val context = LocalContext.current
    var voiceInputResult by remember { mutableStateOf<String?>(null) }
    var isScraping by remember { mutableStateOf(false) }
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val selectedTabArg = navBackStackEntry.value?.arguments?.getString("selectedTab")?.toIntOrNull()

    var selectedTab by rememberSaveable { mutableStateOf(selectedTabArg ?: 0) }

    // Activity Result Launcher for Speech-to-Text
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            voiceInputResult = results?.get(0)
        }
    }

    // Function to start the voice input process
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


    //Permission Launcher (To request RECORD_AUDIO permission)
    var showPermissionDeniedMessage by remember { mutableStateOf(false) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startVoiceInput() // Launch STT if permission is granted
        } else {
            showPermissionDeniedMessage = true
        }
    }


    //Function passed to ChatContent (Checks permission before starting)
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

    // Initialize the service instance and coroutine scope
    val openAIService = remember { OpenAIService() }
    val scope = rememberCoroutineScope()

    // Chat history management
    val chatSessions = remember { mutableStateListOf<ChatSession>() }
    var currentChatId by rememberSaveable { mutableStateOf("") }

    var webViewUrl by remember { mutableStateOf<String?>(null) }

    // Flag to ensure initial data fetch only runs once
    var isInitialLoad by remember { mutableStateOf(true) }

    // Banner message state
    var browserInstructionMessage by remember { mutableStateOf<String?>(null) }

    // The instruction text
    val degreeworksInstruction = "🌐 Please sign in to the Morgan Gateway and navigate directly to your Degreeworks page. When ready, tap the 'Scrape' button."

    // Function to launch the Custom Tab browser
    val launchInAppBrowser: (String) -> Unit = { url ->
        // Set the URL to open the WebView screen
        webViewUrl = url
        // Display the instruction banner
        browserInstructionMessage = degreeworksInstruction
    }

    // Function to close the WebView
    val closeWebView: () -> Unit = {
        webViewUrl = null
        browserInstructionMessage = null // Also dismiss the banner
        isScraping = false
    }

    // Handler for receiving HTML from WebViewScreen
    val handleHtmlScraped: (String) -> Unit = { html ->
        //  Close the WebView
        closeWebView()

        //Show loading indicator
        isScraping = true

        // Start the API call in a coroutine
        scope.launch {
            Log.d("Scrape", "Received HTML. Initiating API call.")

            // Calls the new function in OpenAIService
            val success = openAIService.scrapeAndSaveDegreeData(html)

            // Update UI/Chat based on result
            isScraping = false // Turn off loading

            val resultMessage = if (success) {
                "✅ Degree audit data successfully scraped and saved! You can now ask questions about your degree progress."
            } else {
                "❌ Degree audit scraping failed. Please ensure you are logged into your DegreeWorks page and try again."
            }

            // Add a system message to the current chat (for feedback)
            val currentChat = chatSessions.find { it.id == currentChatId } ?: chatSessions.firstOrNull()
            currentChat?.let { session ->
                session.messages.add(ChatMessage(text = resultMessage, isUser = false))
                // Save the system message to Firestore
                openAIService.saveChatUpdate(session, session.messages.last())
            }
        }
    }


    // Load chat sessions from Firestore on composition or user sign-in
    LaunchedEffect(Unit) {
        if (isInitialLoad) {
            val fetchedSessions = openAIService.fetchChatSessions()

            // Check if any sessions were fetched
            if (fetchedSessions.isNotEmpty()) {
                chatSessions.addAll(fetchedSessions)
                // Set current chat to the most recent one (first in the list)
                currentChatId = chatSessions.first().id
            } else {
                // If no chats, create the default "New Chat" session locally
                val newChat = ChatSession(
                    title = "New Chat",
                    messages = mutableListOf(
                        ChatMessage(
                            text = "Hello! I'm your AI assistant. How can I help you today?",
                            isUser = false
                        )
                    )
                )
                // add it to the list, and it will be saved to Firestore on first message.
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


                            // Professional title styling
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

                            // Professional history button - only on Chat tab
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
                        // Theme toggle with professional styling
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

                        // Only show logout in top bar if not on Profile tab
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

                            // Button to dismiss the instruction once user is done
                            IconButton(onClick = { closeWebView() }) { // Use closeWebView here
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
            // Show the currently selected tab content
            when (selectedTab) {
                0 -> {
                    if (isScraping) {
                        Box( // <-- IF isScraping is TRUE, show this loading box
                            modifier = Modifier
                                .padding(inner)
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ){
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(16.dp))
                                Text("Analyzing Degreeworks data...", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    } else {
                        // Find the chat session
                        val currentChat = chatSessions.find { it.id == currentChatId } ?: chatSessions.firstOrNull()

                        if (currentChat != null) {
                            ChatContent(
                                modifier = Modifier.padding(inner),
                                chatSession = currentChat,
                                onNewChat = {
                                    val newChat = ChatSession(
                                        title = "New Chat",
                                        messages = mutableListOf(
                                            ChatMessage(
                                                text = "Hello! I'm your AI assistant. How can I help you today?",
                                                isUser = false
                                            )
                                        )
                                    )
                                    chatSessions.add(0, newChat)
                                    currentChatId = newChat.id
                                },
                                // Logic to run when a new message (user or bot) is added
                                onMessageSent = {
                                    // This is called AFTER a user/bot message is added to the local list.
                                    currentChat.messages.lastOrNull()?.let { lastMessage ->
                                        scope.launch {
                                            openAIService.saveChatUpdate(currentChat, lastMessage)
                                            // Re-add to the top for local history sorting
                                            chatSessions.remove(currentChat)
                                            chatSessions.add(0, currentChat)
                                        }
                                    }
                                },
                                // Logic to run when the title is changed
                                onTitleChanged = { newTitle ->
                                    val sessionToUpdate = chatSessions.find { it.id == currentChatId }
                                    if (sessionToUpdate != null) {
                                        sessionToUpdate.title = newTitle

                                        // Save the updated title to Firestore
                                        scope.launch {
                                            // Use the existing function, passing the last message or a placeholder
                                            openAIService.saveChatUpdate(
                                                sessionToUpdate,
                                                sessionToUpdate.messages.lastOrNull() ?: ChatMessage("Title changed", false)
                                            )

                                            // Re-sort the local list
                                            chatSessions.remove(sessionToUpdate)
                                            chatSessions.add(0, sessionToUpdate)
                                        }
                                    }
                                },
                                voiceInput = voiceInputResult,
                                onVoiceInputComplete = { voiceInputResult = null },
                                onMicClicked = handleMicClick
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
                    onLogout = onLogout
                )
            }
        }


        // Professional Chat History Modal
        if (showChatHistory) {
            ChatHistoryModal(
                chatSessions = chatSessions,
                currentChatId = currentChatId,
                onChatSelected = { chatId ->
                    currentChatId = chatId
                    showChatHistory = false
                },
                onChatDeleted = { chatId ->
                    // Find the chat session to delete
                    val chatToDelete = chatSessions.find { it.id == chatId }


                    chatToDelete?.let { session ->
                        scope.launch {
                            try {
                                // delete fromm Firestore
                                openAIService.deleteChat(session.id)

                                // Update local state ONLY on success
                                chatSessions.removeAll { it.id == chatId }

                                // Handle selection change
                                if (currentChatId == chatId && chatSessions.isNotEmpty()) {
                                    currentChatId = chatSessions[0].id
                                } else if (chatSessions.isEmpty()) {
                                    // Create new chat if all deleted
                                    val newChat = ChatSession(
                                        title = "New Chat",
                                        messages = mutableListOf(
                                            ChatMessage(
                                                text = "Hello! I'm your AI assistant. How can I help you today?",
                                                isUser = false
                                            )
                                        )
                                    )
                                    chatSessions.add(newChat)
                                    currentChatId = newChat.id
                                }
                            } catch (e: Exception) {
                                Log.e("MainScreen", "Failed to delete chat from Firestore: ${e.message}")
                            }
                        }
                    } ?: run {
                        // If chatToDelete is null, just update local state if needed (shouldn't happen often)
                        chatSessions.removeAll { it.id == chatId }
                    }


                },
                onNewChat = {
                    val newChat = ChatSession(
                        title = "New Chat",
                        messages = mutableListOf(
                            ChatMessage(
                                text = "Hello! I'm your AI assistant. How can I help you today?",
                                isUser = false
                            )
                        )
                    )
                    chatSessions.add(0, newChat)
                    currentChatId = newChat.id
                    showChatHistory = false
                },
                onDismiss = { showChatHistory = false }
            )
        }
    }
}
