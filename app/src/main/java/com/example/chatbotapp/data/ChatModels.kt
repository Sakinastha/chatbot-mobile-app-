// File: app/src/main/java/com/example/chatbotapp/data/ChatModels.kt
package com.example.chatbotapp.data
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.Timestamp
import androidx.annotation.Keep
import kotlin.jvm.JvmField

// Message data class
data class ChatMessage(
    val text: String="",
    @JvmField
    val isUser: Boolean= false,
    val timestamp: String = getCurrentTime(),
    val hasAttachment: Boolean = false
)

// Chat session data class
data class ChatSession(
    @DocumentId
    val id: String = generateChatId(),
    var title: String= "New Chat",
    val messages: MutableList<ChatMessage> = mutableListOf(),
    val createdAt: String = getCurrentTime(),
    @ServerTimestamp
    var lastUpdatedTimestamp: Timestamp? = null)

// Helper functions
fun getCurrentTime(): String {
    return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        .format(java.util.Date())
}

fun generateChatId(): String {
    return "chat_${System.currentTimeMillis()}_${(0..9999).random()}"
}

fun updateChatTitle(chatSession: ChatSession, userMessage: String) {
    // Update title to first user message if it's still "New Chat"
    if (chatSession.title == "New Chat" && userMessage.isNotBlank()) {
        // Take first 30 characters of user message as title
        val newTitle = if (userMessage.length > 30) {
            userMessage.take(30) + "..."
        } else {
            userMessage
        }
        chatSession.title = newTitle
    }
}

fun generateBotResponse(query: String): String {
    return when {
        query.lowercase().contains("hello") || query.lowercase().contains("hi") ->
            "Hello! I'm here to help you with any questions about chatbots, AI, or programming. What would you like to learn about?"
        query.lowercase().contains("help") ->
            "I can assist you with:\n• AI and Machine Learning concepts\n• Chatbot development\n• Programming questions\n• MSU course information\n\nWhat specific topic interests you?"
        query.lowercase().contains("course") ->
            "I can help you with course-related questions! Check out the Curriculum tab for structured learning modules, or ask me about specific topics you'd like to explore."
        else ->
            "That's an interesting question! I'd be happy to help you explore that topic further. Could you provide more details about what specifically you'd like to know?"
    }
}