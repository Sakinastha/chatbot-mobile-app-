@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.chatbotapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chatbotapp.data.ChatMessage
import com.example.chatbotapp.data.ChatSession
import com.example.chatbotapp.data.updateChatTitle
import com.example.chatbotapp.components.SmartChatBubble
import com.example.chatbotapp.components.TypingIndicator
import com.example.chatbotapp.api.OpenAIService
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.saveable.rememberSaveable

// Add the new EditableChatTitle Composable
@Composable
fun EditableChatTitle(
    chatSession: ChatSession,
    onTitleChanged: (String) -> Unit
) {
    // State to manage whether the title is in edit mode
    var isEditing by remember { mutableStateOf(false) }

    // State to hold the temporary text while editing
    var newTitle by rememberSaveable { mutableStateOf(chatSession.title) }

    // Logic to reset the title when the chatSession changes
    LaunchedEffect(chatSession.id) {
        newTitle = chatSession.title
    }

    if (isEditing) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 1. Text Field for editing
            OutlinedTextField(
                value = newTitle,
                onValueChange = { newTitle = it },
                label = { Text("Edit Title") },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    // Save action on 'Done' button press
                    onTitleChanged(newTitle)
                    isEditing = false
                }),
                modifier = Modifier.weight(1f)
            )

            // 2. Save Button
            IconButton(
                onClick = {
                    onTitleChanged(newTitle)
                    isEditing = false
                },
                enabled = newTitle.isNotBlank()
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Save Title",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            //Display Current Title
            Text(
                text = chatSession.title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(end = 8.dp) // Space before the button
            )

            // 2. Edit Button

                IconButton(onClick = { isEditing = true }) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Edit Chat Title",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

        }
    }
}
@Composable
fun ChatContent(
    modifier: Modifier = Modifier,
    chatSession: ChatSession,
    onNewChat: () -> Unit,
    onMessageSent: () -> Unit = {},
    onTitleChanged: (String) -> Unit,
    onMicClicked: () -> Unit,
    voiceInput: String?,
    onVoiceInputComplete: () -> Unit
)
{
    var userInput by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var isTyping by remember { mutableStateOf(false) }
    // When a new voice input string arrives, update the userInput field and signal MainActivity to clear the result
    LaunchedEffect(voiceInput) {
        if (voiceInput != null) {
            // Set the user input field to the recognized speech (overwrites current text)
            userInput = voiceInput.trim()

            // Reset listening state for the UI icon
            isListening = false

            //Tell the parent (MainScreen) the result has been consumed
            onVoiceInputComplete()

        }
    }

    // Create OpenAI service instance
    val openAIService = remember { OpenAIService() }
    val coroutineScope = rememberCoroutineScope()


    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Professional chat header - similar to ChatGPT
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 1.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chat title with professional styling
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            Icons.Filled.SmartToy,
                            contentDescription = "AI Assistant",
                            modifier = Modifier.padding(6.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {

                        // The EditableChatTitle Composable is placed here
                        EditableChatTitle(chatSession, onTitleChanged)

                        Text(
                            text = "${chatSession.messages.size} messages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Compact new chat button
                OutlinedButton(
                    onClick = onNewChat,
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "New Chat",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "New",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        // Chat messages area with proper spacing
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            reverseLayout = true,
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isTyping) {
                item {
                    TypingIndicator()
                }
            }

            items(chatSession.messages.reversed()) { message ->
                SmartChatBubble(message)
            }
        }

        // Professional input area - similar to ChatGPT
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 8.dp,
            shadowElevation = 4.dp
        ) {
            Column {
                if (isTyping) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Input row with better spacing and styling
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Attachment button with professional styling
                    IconButton(
                        onClick = {
                            chatSession.messages.add(ChatMessage("📎 File attached", true, hasAttachment = true))
                            updateChatTitle(chatSession, "📎 File attached")
                            onMessageSent() // Save to Firebase after attachment
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Filled.AttachFile,
                            contentDescription = "Attach file",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Professional text input field
                    OutlinedTextField(
                        value = userInput,
                        onValueChange = { userInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                "Message AI Assistant...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        shape = MaterialTheme.shapes.extraLarge,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Voice input button with better styling
                    IconButton(
                        // When clicked, set local listening state (for visual)
                        // and call the starter function from MainScreen.
                        onClick = {
                            isListening = true
                            onMicClicked()
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            // Use isListening to change the icon/tint, giving the user feedback
                            if (isListening) Icons.Filled.MicOff else Icons.Filled.Mic,
                            contentDescription = if (isListening) "Stop listening" else "Start voice input",
                            tint = if (isListening)
                                MaterialTheme.colorScheme.primary // Use primary to signal active state
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Professional send button
                    FloatingActionButton(
                        onClick = {
                            if (userInput.isNotBlank()) {
                                // Add user message
                                chatSession.messages.add(ChatMessage(userInput, true))
                                updateChatTitle(chatSession, userInput)
                                val query = userInput
                                userInput = ""

                                //Save user message to Firebase immediately
                                onMessageSent()


                                //Show typing indicator
                                isTyping = true

                                //Call OpenAI API
                                coroutineScope.launch {
                                    try {
                                        val response = openAIService.getChatResponse(query)
                                        isTyping = false

                                        // Add AI response to the local list
                                        chatSession.messages.add(ChatMessage(
                                            text = response,
                                            isUser = false
                                        ))

                                        // Save *both* messages to Firebase AT ONCE
                                        onMessageSent()

                                    } catch (e: Exception) {
                                        isTyping = false
                                        chatSession.messages.add(ChatMessage(
                                            text = "Sorry, I encountered an error. Please try again.",
                                            isUser = false
                                        ))
                                        // Save error message to Firebase
                                        onMessageSent()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        containerColor = if (userInput.isBlank())
                            MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.primary,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = if (userInput.isBlank()) 0.dp else 6.dp
                        )
                    ) {
                        Icon(
                            Icons.Filled.Send,
                            contentDescription = "Send message",
                            tint = if (userInput.isBlank())
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}