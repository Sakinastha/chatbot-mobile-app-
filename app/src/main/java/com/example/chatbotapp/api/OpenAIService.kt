package com.example.chatbotapp.api

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import kotlin.math.sqrt
import com.example.chatbotapp.data.ChatSession
import com.example.chatbotapp.data.ChatMessage
import com.google.firebase.auth.FirebaseAuth // Import for getting the cu
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldPath


class OpenAIService {

    private val apiKey = ""
    private val client = OkHttpClient()
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "OpenAIService"

    private val contextCache = mutableMapOf<String, Pair<String, Long>>()
    private val embeddingCache = mutableMapOf<String, List<Double>>()
    private val CACHE_DURATION = 10 * 60 * 1000L
    // Helper to get the current user ID
    private val currentUserId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid
    suspend fun scrapeAndSaveDegreeData(html: String): Boolean {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Log.e("Scrape", "User not authenticated. Cannot save data.")
            return false
        }

        val client = OkHttpClient()
        val mediaType = "application/json; charset=utf-8".toMediaType()

        // Correctly escape the HTML string for JSON payload
        val json = """{"html": ${JSONObject.quote(html)}}"""
        val body = json.toRequestBody(mediaType)

        // IMPORTANT: Change this IP/Port to match your running server setup
        val request = Request.Builder()
            .url("http://192.168.1.240:8000/scrape")
            .post(body)
            .build()

        return try {
            Log.d("Scrape", "Sending HTML to server...")
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseData = response.body?.string()
                Log.i("Scrape", "Server responded successfully.")

                if (responseData != null) {
                    // Assuming the server returns a JSON structure (the scraped data)
                    // We'll save this raw JSON string to Firestore under the user's profile.
                    val db = FirebaseFirestore.getInstance()

                    // Firestore path: users/{userId}
                    val userRef = db.collection("users").document(userId)

                    // Update the user's document with the scraped data
                    userRef.update("degreeAuditData", responseData).await()
                    Log.i("Scrape", "Degree audit data saved to Firestore for user $userId.")
                    true
                } else {
                    Log.e("Scrape", "Server response body was empty.")
                    false
                }
            } else {
                Log.e("Scrape", "Server request failed: ${response.code} - ${response.message}")
                false
            }
        } catch (e: IOException) {
            Log.e("Scrape", "Network failure while sending HTML", e)
            false
        }
    }


    suspend fun deleteChat(chatId: String) {
        // This is the  Firestore deletion logic
        FirebaseFirestore.getInstance()
            .collection("chatSessions")
            .document(chatId)
            .delete()
            .await()
    }

    // function to save/update a chat session and save a new message
    suspend fun saveChatUpdate(chatSession: ChatSession, newMessage: ChatMessage): Boolean {
        val userId = currentUserId ?: return false // Must have a logged-in user

        return try {
            //  Get the reference to the specific chat document
            val chatRef = db.collection("users").document(userId)
                .collection("chats").document(chatSession.id)

            // Update the ChatSession document (mostly to update the ServerTimestamp)
            val sessionUpdates = mapOf(
                "title" to chatSession.title,
                // ServerTimestamp will be updated automatically due to @ServerTimestamp in ChatSession
                "lastUpdatedTimestamp" to com.google.firebase.Timestamp.now()
            )
            chatRef.set(sessionUpdates, com.google.firebase.firestore.SetOptions.merge()).await()

            // Add the new message to the 'messages' subcollection
            chatRef.collection("messages").add(newMessage).await()

            Log.d(TAG, "Chat session and message saved successfully.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving chat update: ${e.message}")
            false
        }
    }

    // function to fetch a user's chat sessions
    suspend fun fetchChatSessions(): List<ChatSession> {
        val userId = currentUserId ?: return emptyList()
        val fetchedSessions = mutableListOf<ChatSession>() // 👈 1. Use a mutable list to build the result

        return try {
            //  Fetch all ChatSession documents (title, metadata)
            val sessionsSnapshot = db.collection("users").document(userId)
                .collection("chats")
                .orderBy("lastUpdatedTimestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            // Iterate through documents and fetch messages for each
            for (document in sessionsSnapshot.documents) {
                // Convert document to ChatSession object
                val session = document.toObject(ChatSession::class.java)

                if (session != null) {
                    // Fetch messages subcollection for the current session
                    val messagesSnapshot = document.reference.collection("messages")
                        // Ensure you are ordering the messages to display them chronologically
                        .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                        .get()
                        .await()

                    // Convert messages documents to ChatMessage objects and add to the session's list
                    val messages = messagesSnapshot.documents.mapNotNull { msgDoc ->
                        msgDoc.toObject(ChatMessage::class.java)
                    }
                    val sortedMessages = messages.sortedWith(
                        compareBy<ChatMessage> { it.timestamp } // Primary: Sort by timestamp ascending
                            .thenBy { if (it.isUser) 0 else 1}     // Secondary: User (0) comes before AI (1)
                    )

                    // Add the fetched messages to the session object
                    session.messages.clear()
                    session.messages.addAll(sortedMessages)

                    fetchedSessions.add(session)
                }
            }

            // Return the list you built outside the loop
            fetchedSessions

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching chat sessions: ${e.message}")
            emptyList()
        }
    }


    // Query intent types
    private enum class QueryIntent {
        ROLE_QUERY,      // "who is the chair"
        PERSON_QUERY,    // "who is Dr. Paul Wang"
        GENERAL_QUERY    // "what are the office hours"
    }

    private data class QueryAnalysis(
        val intent: QueryIntent,
        val roleKeyword: String? = null,
        val personName: String? = null,
        val keywords: List<String> = emptyList()
    )

    private fun handleCasualConversation(message: String): String? {
        val msg = message.lowercase().trim()
        return when {
            msg in listOf("hi", "hello", "hey", "greetings", "howdy", "yo") ->
                "Hello! I'm your Morgan State University AI assistant. How can I help you?"


            msg.contains("good morning") || msg in listOf("morning", "gm") ->
                "Good morning! I hope you have a wonderful day ahead. How can I assist you with Morgan State University today?"

            msg.contains("good afternoon") || msg == "afternoon" ->
                "Good afternoon! How can I help you with Morgan State University today?"

            msg.contains("good evening") || msg == "evening" ->
                "Good evening! How can I assist you with Morgan State University today?"

            msg.contains("good night") || msg in listOf("goodnight", "night") ->
                "Good night! If you need anything else, feel free to reach out anytime. Sleep well!"


            msg.contains("how are you") || msg.contains("how r u") || msg.contains("how are u") ->
                "I'm doing great! Thank you for asking. How can I help you with Morgan State University?"

            msg.contains("how's it going") || msg.contains("how is it going") || msg == "sup" || msg == "wassup" ->
                "Everything's going well! How can I assist you today?"


            msg in listOf("bye", "goodbye", "bye bye", "see you", "see ya", "later", "catch you later", "take care") ->
                "Goodbye! Come back anytime you need help. Take care!"

            msg.contains("have a good day") || msg.contains("have a great day") || msg.contains("have a nice day") ->
                "Thank you! You have a wonderful day too! Feel free to come back if you need anything else."

            msg.contains("have a good night") || msg.contains("have a great night") ->
                "Thank you! Have a great night! Reach out anytime you need assistance."


            msg in listOf("thanks", "thank you", "thx", "ty", "tysm", "thank u", "appreciate it") ->
                "You're welcome! I'm always here to help if you need anything else."

            msg.contains("thanks a lot") || msg.contains("thank you so much") ->
                "You're very welcome! Happy to help anytime."


            msg.contains("nice to meet you") || msg.contains("pleased to meet you") ->
                "Nice to meet you too! I'm here to help you with any questions about Morgan State University."

            msg in listOf("ok", "okay", "alright", "got it", "understood") ->
                "Great! Let me know if you need anything else."

            msg in listOf("yes", "yeah", "yep", "sure", "yup") ->
                "Wonderful! What would you like to know about Morgan State University?"

            msg in listOf("no", "nope", "nah", "not really") ->
                "No problem! If you change your mind or need anything, I'm here to help."


            msg.contains("i need help") || msg.contains("help me") || msg == "help" ->
                "I'm here to help! You can ask me about classes, faculty, departments, registration, campus resources, and much more. What would you like to know?"

            msg.contains("what can you do") || msg.contains("what do you do") ->
                "I can help you find information about Morgan State University including academic programs, faculty contacts, registration dates, campus resources, and more. What would you like to know?"


            msg.contains("good job") || msg.contains("well done") || msg.contains("nice work") || msg == "great" ->
                "Thank you for the kind words! I'm here whenever you need assistance."

            msg.contains("you're helpful") || msg.contains("you are helpful") || msg.contains("very helpful") ->
                "I'm glad I could help! Feel free to ask me anything else about Morgan State University."


            msg in listOf("sorry", "my bad", "oops", "apologies") ->
                "No worries at all! How can I assist you today?"
            else -> null
        }
    }

    private fun analyzeQuery(question: String): QueryAnalysis {
        val q = question.lowercase()

        // Check for role queries first
        val roleKeywords = mapOf(
            "chair" to listOf("chair", "chairperson", "department head"),
            "director" to listOf("director", "program director"),
            "dean" to listOf("dean"),
            "advisor" to listOf("advisor", "adviser", "advising")
        )

        for ((role, variations) in roleKeywords) {
            for (variation in variations) {
                if (q.contains(variation)) {
                    val rolePattern = Regex("(?:who is|what is|find|get|contact).{0,20}(?:the )?$variation")
                    if (rolePattern.find(q) != null) {
                        Log.d(TAG, "🎯 ROLE QUERY detected: $role")
                        return QueryAnalysis(
                            intent = QueryIntent.ROLE_QUERY,
                            roleKeyword = role,
                            keywords = variations
                        )
                    }
                }
            }
        }

        // Check for person name queries
        val personName = extractPersonName(question)
        if (personName != null && !roleKeywords.values.flatten().any { personName.contains(it) }) {
            Log.d(TAG, "👤 PERSON QUERY detected: $personName")
            return QueryAnalysis(
                intent = QueryIntent.PERSON_QUERY,
                personName = personName
            )
        }

        Log.d(TAG, "📝 GENERAL QUERY detected")
        return QueryAnalysis(intent = QueryIntent.GENERAL_QUERY)
    }

    private fun extractPersonName(question: String): String? {
        val q = question.lowercase()

        // Skip if it contains role keywords
        val roleWords = listOf("chair", "director", "dean", "head", "advisor", "adviser")
        if (roleWords.any { q.contains(it) }) {
            return null
        }

        // Pattern: "who is [name]"
        val whoMatch = Regex("who\\s+is\\s+(?:dr\\.?\\s+)?([a-z]+\\s+[a-z]+)").find(q)
        if (whoMatch != null) {
            return whoMatch.groupValues[1].trim()
        }

        // Look for "Dr. FirstName LastName" pattern
        val drPattern = Regex("dr\\.?\\s+([a-z]+\\s+[a-z]+)").find(q)
        if (drPattern != null) {
            return drPattern.groupValues[1].trim()
        }

        // Look for capitalized words (names)
        val words = question.split(Regex("\\s+"))
        val skipWords = setOf("who", "is", "the", "what", "where", "tell", "me", "about",
            "morgan", "state", "university", "computer", "science", "department", "dr", "doctor")

        val nameWords = mutableListOf<String>()
        for (word in words) {
            val clean = word.replace(Regex("[^a-zA-Z]"), "")
            if (clean.length > 1 && clean.lowercase() !in skipWords &&
                clean[0].isUpperCase()) {
                nameWords.add(clean.lowercase())
                if (nameWords.size >= 2) break
            }
        }

        return if (nameWords.size >= 2) nameWords.joinToString(" ") else null
    }

    private fun extractKeywords(query: String, analysis: QueryAnalysis): List<String> {
        val keywords = mutableListOf<String>()

        when (analysis.intent) {
            QueryIntent.ROLE_QUERY -> {
                analysis.roleKeyword?.let { role ->
                    when (role) {
                        "chair" -> keywords.addAll(listOf("chair", "chairperson", "department head", "head"))
                        "director" -> keywords.addAll(listOf("director", "program director"))
                        "dean" -> keywords.add("dean")
                        "advisor" -> keywords.addAll(listOf("advisor", "adviser", "advising"))
                        else -> {}
                    }
                }
            }
            QueryIntent.PERSON_QUERY -> {
                val q = query.lowercase()
                if (q.contains("contact") || q.contains("email") || q.contains("phone")) {
                    keywords.addAll(listOf("email", "phone", "office", "contact"))
                }
            }
            QueryIntent.GENERAL_QUERY -> {
                val q = query.lowercase()
                when {
                    q.contains("contact") -> keywords.addAll(listOf("email", "phone", "contact"))
                    q.contains("office") -> keywords.addAll(listOf("office", "room", "location"))
                    q.contains("hours") -> keywords.add("hours")
                }
            }
        }

        return keywords
    }

    private suspend fun getEmbedding(text: String): List<Double> {
        embeddingCache[text]?.let { return it }

        return try {
            val jsonBody = JSONObject().apply {
                put("model", "text-embedding-3-small")
                put("input", text)
            }

            val body = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://api.openai.com/v1/embeddings")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(body)
                .build()

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful) {
                    return@withContext emptyList<Double>()
                }

                val json = JSONObject(responseBody ?: "")
                val embeddingArray = json.getJSONArray("data")
                    .getJSONObject(0)
                    .getJSONArray("embedding")

                val embedding = mutableListOf<Double>()
                for (i in 0 until embeddingArray.length()) {
                    embedding.add(embeddingArray.getDouble(i))
                }

                embeddingCache[text] = embedding
                embedding
            }
        } catch (e: Exception) {
            Log.e(TAG, "Embedding error", e)
            emptyList()
        }
    }

    private suspend fun getEnhancedEmbedding(query: String, analysis: QueryAnalysis): List<Double> {
        val keywords = extractKeywords(query, analysis)
        val enhancedQuery = if (keywords.isNotEmpty()) {
            "$query ${keywords.joinToString(" ")}"
        } else {
            query
        }
        return getEmbedding(enhancedQuery)
    }

    private fun cosineSimilarity(vec1: List<Double>, vec2: List<Double>): Double {
        if (vec1.isEmpty() || vec2.isEmpty() || vec1.size != vec2.size) return 0.0

        var dotProduct = 0.0
        var norm1 = 0.0
        var norm2 = 0.0

        for (i in vec1.indices) {
            dotProduct += vec1[i] * vec2[i]
            norm1 += vec1[i] * vec1[i]
            norm2 += vec2[i] * vec2[i]
        }

        val denominator = sqrt(norm1) * sqrt(norm2)
        return if (denominator > 0) dotProduct / denominator else 0.0
    }

    private fun fuzzyContains(text: String, searchTerm: String): Boolean {
        val textLower = text.lowercase()
        val termLower = searchTerm.lowercase()

        // Exact match
        if (textLower.contains(termLower)) return true

        // Check each word
        val termParts = termLower.split(" ").filter { it.length > 2 }
        for (part in termParts) {
            if (textLower.contains(part)) return true
        }

        // Fuzzy match
        val textWords = textLower.split(Regex("\\s+"))
        for (part in termParts) {
            for (word in textWords) {
                if (levenshteinDistance(word, part) <= 1) return true
            }
        }

        return false
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        if (s1.length < 2 || s2.length < 2) return if (s1 == s2) 0 else 2

        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }

        return dp[s1.length][s2.length]
    }

    private fun extractRoleSection(docText: String, roleKeyword: String): String {
        val lines = docText.split("\n")
        val relevantSections = mutableListOf<String>()
        var currentSection = mutableListOf<String>()
        var inRoleSection = false

        for (i in lines.indices) {
            val line = lines[i]
            val lineLower = line.lowercase()
            val trimmedLine = line.trim()

            // Section boundary detection
            val isSectionBoundary = trimmedLine.isEmpty() ||
                    trimmedLine.matches(Regex("^[=\\-#*]{3,}.*")) ||
                    trimmedLine.matches(Regex("^###.*"))

            if (isSectionBoundary) {
                if (inRoleSection && currentSection.isNotEmpty()) {
                    relevantSections.add(currentSection.joinToString("\n"))
                }
                currentSection.clear()
                inRoleSection = false
                continue
            }

            // Check if this line contains the role keyword
            if (lineLower.contains(roleKeyword) ||
                (roleKeyword == "chair" && (lineLower.contains("chairperson") || lineLower.contains("department head")))) {
                inRoleSection = true
            }

            if (inRoleSection) {
                currentSection.add(line)
            }
        }

        if (inRoleSection && currentSection.isNotEmpty()) {
            relevantSections.add(currentSection.joinToString("\n"))
        }

        return relevantSections.joinToString("\n\n")
    }

    private fun extractPersonSection(docText: String, personName: String): String {
        val lines = docText.split("\n")
        val relevantSections = mutableListOf<String>()
        var currentSection = mutableListOf<String>()
        var inPersonSection = false

        for (i in lines.indices) {
            val line = lines[i]
            val trimmedLine = line.trim()

            val isSectionBoundary = trimmedLine.isEmpty() ||
                    trimmedLine.matches(Regex("^[=\\-#*]{3,}.*"))

            if (isSectionBoundary) {
                if (inPersonSection && currentSection.isNotEmpty()) {
                    relevantSections.add(currentSection.joinToString("\n"))
                }
                currentSection.clear()
                inPersonSection = false
                continue
            }

            if (fuzzyContains(line, personName)) {
                inPersonSection = true
            }

            if (inPersonSection) {
                currentSection.add(line)
            }
        }

        if (inPersonSection && currentSection.isNotEmpty()) {
            relevantSections.add(currentSection.joinToString("\n"))
        }

        return relevantSections.joinToString("\n\n")
    }

    private suspend fun getRelevantContext(userQuestion: String, analysis: QueryAnalysis): String {
        val cacheKey = userQuestion.lowercase().take(50)
        contextCache[cacheKey]?.let { (cached, time) ->
            if (System.currentTimeMillis() - time < CACHE_DURATION) {
                Log.d(TAG, "⚡ Using cache")
                return cached
            }
        }

        return try {
            Log.d(TAG, "🤖 HYBRID AI search...")

            val questionEmbedding = getEnhancedEmbedding(userQuestion, analysis)
            if (questionEmbedding.isEmpty()) {
                return "Error processing question."
            }

            val snapshot = db.collection("knowledge_base").get().await()
            val similarities = mutableListOf<Triple<String, Double, String>>()

            for (doc in snapshot.documents) {
                val docId = doc.id
                val docData = doc.data ?: continue
                val docText = formatData(docData)

                val docEmbedding = getEmbedding(docText.take(2000))
                if (docEmbedding.isEmpty()) continue

                val baseSim = cosineSimilarity(questionEmbedding, docEmbedding)

                var boost = 0.0
                val docLower = docText.lowercase()

                when (analysis.intent) {
                    QueryIntent.ROLE_QUERY -> {
                        analysis.roleKeyword?.let { role ->
                            if (docLower.contains(role) ||
                                (role == "chair" && (docLower.contains("chairperson") || docLower.contains("department head")))) {
                                boost = 0.6
                                Log.d(TAG, "🎯 ROLE '$role' found in $docId +BOOST($boost)")
                            }
                        }
                    }
                    QueryIntent.PERSON_QUERY -> {
                        analysis.personName?.let { name ->
                            if (fuzzyContains(docText, name)) {
                                boost = 0.4
                                Log.d(TAG, "🎯 PERSON '$name' found in $docId +BOOST($boost)")
                            }
                        }
                    }
                    QueryIntent.GENERAL_QUERY -> {
                        boost = 0.0
                    }
                }

                val finalSim = (baseSim + boost).coerceAtMost(1.0)
                similarities.add(Triple(docId, finalSim, docText))
                Log.d(TAG, "📊 $docId: ${(finalSim * 100).toInt()}%")
            }

            val topDocs = similarities.sortedByDescending { it.second }.take(3)
            Log.d(TAG, "🏆 Top: ${topDocs.map { "${it.first}(${(it.second * 100).toInt()}%)" }}")

            val contextSnippets = mutableListOf<Triple<Double, String, String>>()

            when (analysis.intent) {
                QueryIntent.ROLE_QUERY -> {
                    analysis.roleKeyword?.let { role ->
                        topDocs.forEach { (docId, sim, docText) ->
                            val section = extractRoleSection(docText, role)
                            if (section.isNotEmpty()) {
                                contextSnippets.add(Triple(sim, docId, section))
                            }
                        }
                    }
                }
                QueryIntent.PERSON_QUERY -> {
                    analysis.personName?.let { name ->
                        topDocs.forEach { (docId, sim, docText) ->
                            if (fuzzyContains(docText, name)) {
                                val section = extractPersonSection(docText, name)
                                if (section.isNotEmpty()) {
                                    contextSnippets.add(Triple(sim, docId, section))
                                }
                            }
                        }
                    }
                }
                QueryIntent.GENERAL_QUERY -> {
                    topDocs.forEach { (docId, sim, docText) ->
                        contextSnippets.add(Triple(sim, docId, docText.take(2000)))
                    }
                }
            }

            // Add general context if needed
            topDocs.forEach { (docId, sim, docText) ->
                val alreadyAdded = contextSnippets.any { it.second == docId }
                if (!alreadyAdded) {
                    contextSnippets.add(Triple(sim, docId, docText.take(1500)))
                }
            }

            val sortedSnippets = contextSnippets.sortedByDescending { it.first }

            val context = StringBuilder()
            sortedSnippets.forEach { (sim, docId, text) ->
                context.append("\n=== $docId (${(sim * 100).toInt()}%) ===\n")
                context.append(text).append("\n")
            }

            val result = context.toString()
            val limited = if (result.length > 6000) result.take(6000) else result

            Log.d(TAG, "✅ Context: ${limited.length} chars")
            contextCache[cacheKey] = Pair(limited, System.currentTimeMillis())
            limited

        } catch (e: Exception) {
            Log.e(TAG, "Error", e)
            "Error accessing database."
        }
    }

    private fun formatData(data: Map<String, Any>?): String {
        if (data == null) return ""
        val sb = StringBuilder()

        fun format(k: String, v: Any, indent: String = "") {
            when (v) {
                is Map<*, *> -> {
                    sb.append("$indent$k:\n")
                    v.forEach { (key, value) ->
                        if (value != null) format(key.toString(), value, "$indent  ")
                    }
                }
                is List<*> -> sb.append("$indent$k: ${v.joinToString(", ")}\n")
                else -> sb.append("$indent$k: $v\n")
            }
        }

        data.forEach { (k, v) -> format(k, v) }
        return sb.toString()
    }

    private fun buildSystemPrompt(context: String, analysis: QueryAnalysis): String {
        val baseInstructions = """
            You are Morgan State University's AI assistant.
            
            CRITICAL FORMATTING RULES:
            ❌ DO NOT use any asterisks (*) in your response
            ❌ DO NOT use markdown formatting like **bold** or *italic*
            ❌ DO NOT use any special formatting symbols
            ✅ Write in plain, natural text without any formatting
            ✅ Use simple punctuation only: periods, commas, colons
            
            INSTRUCTIONS:
            ✅ Answer using ONLY the knowledge base below
            ✅ The knowledge base is ordered by relevance - prioritize the FIRST document
            ✅ Extract exact details: names, emails, phones, dates, URLs
            ✅ Write responses in plain text like a normal conversation
            ✅ Be conversational and helpful
            ✅ If you cannot find an exact match, provide the closest related information from the knowledge base
            ✅ Always try to be helpful by providing the most relevant information available
            
            ❌ Don't make up information
            ❌ Don't confuse different people
            ❌ Don't use asterisks or any formatting symbols
            ❌ Don't say "I don't have that information" - instead provide the closest related information
        """.trimIndent()

        val specificInstructions = when (analysis.intent) {
            QueryIntent.ROLE_QUERY -> {
                """
                
                ROLE QUERY INSTRUCTIONS:
                ✅ Look for the role keyword: "${analysis.roleKeyword}"
                ✅ Find the person who holds this position
                ✅ Provide their name, title, contact information in plain text sentences
                ✅ DO NOT confuse with other faculty members
                ✅ Write naturally without any formatting symbols
                ✅ If the exact role isn't found, provide information about related positions or the department
                
                Example format: The Chair of the Computer Science Department is Dr. Paul Wang. You can reach him at paul.wang@morgan.edu or call (443) 885-4508. His office is located in McMechen Hall 507.
                """.trimIndent()
            }
            QueryIntent.PERSON_QUERY -> {
                """
                
                PERSON QUERY INSTRUCTIONS:
                ✅ Focus on: "${analysis.personName}"
                ✅ Provide their title, role, and contact details in plain text
                ✅ Write in natural, conversational sentences without formatting
                ✅ If the exact person isn't found, provide information about similar faculty or staff members
                """.trimIndent()
            }
            QueryIntent.GENERAL_QUERY -> {
                """
                
                GENERAL QUERY INSTRUCTIONS:
                ✅ Provide the most relevant information from the knowledge base
                ✅ If an exact answer isn't available, provide related information that might be helpful
                ✅ Be conversational and guide the user to the right resources
                ✅ Use plain text without any formatting symbols
                """.trimIndent()
            }
        }

        return """
            $baseInstructions
            $specificInstructions
            
            KNOWLEDGE BASE (ordered by relevance):
            $context
            
            Answer accurately and naturally from the information above in PLAIN TEXT ONLY. DO NOT use asterisks or any markdown formatting whatsoever.
        """.trimIndent()
    }

    suspend fun getChatResponse(userMessage: String): String {
        return try {
            handleCasualConversation(userMessage)?.let { return it }

            val analysis = analyzeQuery(userMessage)
            val context = getRelevantContext(userMessage, analysis)
            val systemPrompt = buildSystemPrompt(context, analysis)

            val jsonBody = JSONObject().apply {
                put("model", "gpt-3.5-turbo")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userMessage)
                    })
                })
                put("max_tokens", 1000)
                put("temperature", 0.3)
            }

            val body = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(body)
                .build()

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful) {
                    return@withContext "Sorry, connection issue. Try again."
                }

                val json = JSONObject(responseBody ?: "")
                val message = json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")

                Log.d(TAG, "✅ Response ready")
                message.trim()
            }

        } catch (e: IOException) {
            "Network error. Check connection."
        } catch (e: Exception) {
            Log.e(TAG, "Error", e)
            "Something went wrong. Try again."
        }
    }
}
