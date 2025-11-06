package com.example.chatbotapp.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatbotapp.data.ChatMessage
import java.util.regex.Pattern

import com.example.chatbotapp.R

@Composable
fun SmartChatBubble(message: ChatMessage) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isUser) {
            // Bot avatar
            Surface(
                // --- INCREASED SIZE HERE ---
                modifier = Modifier.size(40.dp), // Increased size from 32.dp to 40.dp
                shape = RoundedCornerShape(20.dp), // Adjusted shape to match new size
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.chat_icon),
                    contentDescription = "AI Assistant Avatar (Robot)",
                    // Slightly adjusted padding for the larger icon
                    modifier = Modifier.padding(4.dp),
                    tint = Color.Unspecified
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Message bubble
        Surface(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            color = if (message.isUser)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Parse and display text with clickable links
                ClickableText(
                    text = message.text,
                    isUser = message.isUser,
                    onClick = { url ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }
                )

                if (message.hasAttachment) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.AttachFile,
                            contentDescription = "Attachment",
                            modifier = Modifier.size(14.dp),
                            tint = if (message.isUser)
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Attachment",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (message.isUser)
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        if (message.isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            // User avatar
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = "User",
                    modifier = Modifier.padding(6.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun ClickableText(
    text: String,
    isUser: Boolean,
    onClick: (String) -> Unit
) {
    val textColor = if (isUser)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    val linkColor = if (isUser)
        Color(0xFF90CAF9) // Light blue for user messages
    else
        MaterialTheme.colorScheme.primary

    // Regex patterns for URLs, emails, and phone numbers
    val urlPattern = Pattern.compile(
        "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)",
        Pattern.CASE_INSENSITIVE
    )
    val emailPattern = Pattern.compile(
        "([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})",
        Pattern.CASE_INSENSITIVE
    )
    val phonePattern = Pattern.compile(
        "(\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}"
    )

    // Parse the text and create annotated string
    val annotatedString = buildAnnotatedString {
        var lastIndex = 0
        val text = text

        // Find all matches
        val matches = mutableListOf<Triple<Int, Int, String>>() // start, end, type

        // Find URLs
        val urlMatcher = urlPattern.matcher(text)
        while (urlMatcher.find()) {
            matches.add(Triple(urlMatcher.start(), urlMatcher.end(), "url"))
        }

        // Find emails
        val emailMatcher = emailPattern.matcher(text)
        while (emailMatcher.find()) {
            matches.add(Triple(emailMatcher.start(), emailMatcher.end(), "email"))
        }

        // Find phone numbers
        val phoneMatcher = phonePattern.matcher(text)
        while (phoneMatcher.find()) {
            matches.add(Triple(phoneMatcher.start(), phoneMatcher.end(), "phone"))
        }

        // Sort matches by position
        matches.sortBy { it.first }

        // Build the annotated string
        for ((start, end, type) in matches) {
            // Add text before the match
            if (start > lastIndex) {
                withStyle(style = SpanStyle(color = textColor)) {
                    append(text.substring(lastIndex, start))
                }
            }

            // Add the clickable link
            val matchText = text.substring(start, end)
            pushStringAnnotation(tag = type, annotation = matchText)
            withStyle(
                style = SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Medium
                )
            ) {
                append(matchText)
            }
            pop()

            lastIndex = end
        }

        // Add remaining text
        if (lastIndex < text.length) {
            withStyle(style = SpanStyle(color = textColor)) {
                append(text.substring(lastIndex))
            }
        }
    }

    // Display the text with click handling
    androidx.compose.foundation.text.ClickableText(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium.copy(
            lineHeight = 20.sp
        ),
        onClick = { offset ->
            // Check which annotation was clicked
            annotatedString.getStringAnnotations(offset, offset).firstOrNull()?.let { annotation ->
                val clickedText = annotation.item
                when (annotation.tag) {
                    "url" -> onClick(clickedText)
                    "email" -> onClick("mailto:$clickedText")
                    "phone" -> onClick("tel:${clickedText.replace("[^0-9+]".toRegex(), "")}")
                }
            }
        }
    )
}