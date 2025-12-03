package com.example.chatbotapp.api

import android.util.Log
import com.example.chatbotapp.data.ChatMessage
import com.example.chatbotapp.data.ChatSession
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

import com.example.chatbotapp.data.CourseEntry

import com.example.chatbotapp.data.StudentProfile

class OpenAIService {

    private val apiKey = "api key"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val db = FirebaseFirestore.getInstance()
    private val TAG = "OpenAIService"

    private val auth = FirebaseAuth.getInstance()

    private val contextCache = mutableMapOf<String, Pair<String, Long>>()
    private val embeddingCache = mutableMapOf<String, List<Double>>()
    private val CACHE_DURATION = 10 * 60 * 1000L

    private val currentUserId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    suspend fun scrapeAndSaveDegreeData(html: String): Boolean {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Log.e("Scrape", "User not authenticated. Cannot save data.")
            return false
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val json = """{"html": ${JSONObject.quote(html)}}"""
        val body = json.toRequestBody(mediaType)

        val request = Request.Builder()
            .url("http://192.168.1.161:8000/scrape")
            .post(body)
            .build()

        return try {
            Log.d("Scrape", "Sending HTML to server...")
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseData = response.body?.string()
                Log.i("Scrape", "Server responded successfully.")

                if (responseData != null) {
                    val db = FirebaseFirestore.getInstance()
                    val userRef = db.collection("users").document(userId)
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
        try {
            FirebaseFirestore.getInstance()
                .collection("chatSessions")
                .document(chatId)
                .delete()
                .await()
            Log.d(TAG, "Chat deleted: $chatId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting chat: ${e.message}")
        }
    }

    suspend fun saveChatUpdate(session: ChatSession, message: ChatMessage) {
        withContext(Dispatchers.IO) {
            try {
                val userId = auth.currentUser?.uid ?: return@withContext

                // Add small delay to prevent conflicts
                delay(100)

                db.collection("users")
                    .document(userId)
                    .collection("chats")
                    .document(session.id)
                    .set(session, SetOptions.merge())
                    .await()

                Log.d("OpenAIService", "Chat saved successfully")
            } catch (e: Exception) {
                Log.e("OpenAIService", "Error saving chat: ${e.message}")
                // Retry once after delay
                delay(500)
                try {
                    val userId = auth.currentUser?.uid ?: return@withContext
                    db.collection("users")
                        .document(userId)
                        .collection("chats")
                        .document(session.id)
                        .set(session, SetOptions.merge())
                        .await()
                } catch (retryError: Exception) {
                    Log.e("OpenAIService", "Retry failed: ${retryError.message}")
                }
            }
        }
    }

    suspend fun fetchChatSessions(): List<ChatSession> {
        val userId = currentUserId ?: return emptyList()
        val fetchedSessions = mutableListOf<ChatSession>()

        return try {
            val sessionsSnapshot = db.collection("users").document(userId)
                .collection("chats")
                .orderBy("lastUpdatedTimestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            for (document in sessionsSnapshot.documents) {
                val session = document.toObject(ChatSession::class.java)

                if (session != null) {
                    val messagesSnapshot = document.reference.collection("messages")
                        .orderBy("timestamp", Query.Direction.ASCENDING)
                        .get()
                        .await()

                    val messages = messagesSnapshot.documents.mapNotNull { msgDoc ->
                        msgDoc.toObject(ChatMessage::class.java)
                    }
                    val sortedMessages = messages.sortedWith(
                        compareBy<ChatMessage> { it.timestamp }
                            .thenBy { if (it.isUser) 0 else 1 }
                    )

                    session.messages.clear()
                    session.messages.addAll(sortedMessages)
                    fetchedSessions.add(session)
                }
            }

            fetchedSessions

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching chat sessions: ${e.message}")
            emptyList()
        }
    }

    private enum class QueryIntent {
        ROLE_QUERY,
        PERSON_QUERY,
        GENERAL_QUERY
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

        val roleWords = listOf("chair", "director", "dean", "head", "advisor", "adviser")
        if (roleWords.any { q.contains(it) }) {
            return null
        }

        val whoMatch = Regex("who\\s+is\\s+(?:dr\\.?\\s+)?([a-z]+\\s+[a-z]+)").find(q)
        if (whoMatch != null) {
            return whoMatch.groupValues[1].trim()
        }

        val drPattern = Regex("dr\\.?\\s+([a-z]+\\s+[a-z]+)").find(q)
        if (drPattern != null) {
            return drPattern.groupValues[1].trim()
        }

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
        if (text.isEmpty()) return emptyList()

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

            withContext(Dispatchers.IO) {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful) {
                    Log.e(TAG, "Embedding API error: ${response.code}")
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
            Log.e(TAG, "Embedding error: ${e.message}", e)
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

        if (textLower.contains(termLower)) return true

        val termParts = termLower.split(" ").filter { it.length > 2 }
        for (part in termParts) {
            if (textLower.contains(part)) return true
        }

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

        for (line in lines) {
            val lineLower = line.lowercase()
            val trimmedLine = line.trim()

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



    private suspend fun getStudentProfileSummary(): String {
        val uid = currentUserId ?: return ""

        return try {
            val doc = db.collection("users")
                .document(uid)
                .collection("degreeworks")
                .document("latest")
                .get()
                .await()

            if (!doc.exists()) return ""

            val data = doc.data ?: return ""
            val preview = data["json_preview"] as? Map<*, *> ?: return ""

            val sb = StringBuilder()

            // Student metadata
            val major = preview["major"] as? String
            val classification = preview["classification"] as? String
            val advisor = preview["advisor"] as? String
            val standing = preview["academic_standing"] as? String
            val graduationStatus = preview["graduation_status"] as? String

            if (major != null) {
                sb.append("You are a $major major")
                if (classification != null) {
                    sb.append(" ($classification)")
                }
                sb.append(". ")
            }

            if (advisor != null) {
                sb.append("Your academic advisor is $advisor. ")
            }

            // GPA and credits
            val gpa = (preview["gpa"] as? Number)?.toDouble()
            val transferHours = (preview["transfer_hours"] as? Number)?.toInt()
            val totalCompletedCredits = (preview["total_completed_credits"] as? Number)?.toDouble()

            if (gpa != null) {
                sb.append("Your GPA is ").append(String.format("%.3f", gpa)).append(". ")
            }

            if (transferHours != null) {
                sb.append("You transferred $transferHours credits from previous institutions. ")
            }

            if (totalCompletedCredits != null && totalCompletedCredits > 0.0) {
                sb.append("At Morgan State, you have completed ").append(totalCompletedCredits).append(" credits. ")
            }

            // CURRENT SEMESTER
            val currentTerm = preview["current_term"] as? String
            val currentTermCredits = (preview["current_term_credits"] as? Number)?.toDouble()
            val currentTermCourses = (preview["current_term_courses"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

            if (!currentTerm.isNullOrBlank() && currentTermCourses.isNotEmpty()) {
                sb.append("\n\nCURRENT SEMESTER ($currentTerm):\n")
                sb.append("You are currently taking $currentTermCredits credits:\n")
                currentTermCourses.forEach { course ->
                    sb.append("- $course\n")
                }
            }

            // Add semester history...
            val semesters = preview["semesters"] as? List<*>
            if (semesters != null && semesters.isNotEmpty()) {
                sb.append("\n\nCOMPLETED COURSE HISTORY:\n")

                semesters.forEach { semesterObj ->
                    val semester = semesterObj as? Map<*, *>
                    if (semester != null) {
                        val term = semester["term"] as? String
                        val courses = semester["courses"] as? List<*>
                        val totalCredits = (semester["total_credits"] as? Number)?.toDouble()

                        if (term != null && courses != null && courses.isNotEmpty()) {
                            sb.append("\n$term ($totalCredits credits):\n")

                            courses.forEach { courseObj ->
                                val course = courseObj as? Map<*, *>
                                if (course != null) {
                                    val code = course["course"] as? String
                                    val title = course["title"] as? String
                                    val grade = course["grade"] as? String

                                    sb.append("  - $code")
                                    if (grade != null && grade != "IP") {
                                        sb.append(" (Grade: $grade)")
                                    }
                                    sb.append("\n")
                                }
                            }
                        }
                    }
                }
            }

            sb.toString().trim()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading student profile: ${e.message}", e)
            ""
        }
    }

    private fun extractPersonSection(docText: String, personName: String): String {
        val lines = docText.split("\n")
        val relevantSections = mutableListOf<String>()
        var currentSection = mutableListOf<String>()
        var inPersonSection = false

        for (line in lines) {
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
                Log.d(TAG, "⚡ Using cached context")
                return cached
            }
        }

        return try {
            Log.d(TAG, "🤖 Searching knowledge base...")

            val questionEmbedding = getEnhancedEmbedding(userQuestion, analysis)
            if (questionEmbedding.isEmpty()) {
                Log.e(TAG, "Failed to generate question embedding")
                return "Unable to process your question. Please try again."
            }

            val snapshot = db.collection("knowledge_base")
                .limit(8)
                .get()
                .await()

            if (snapshot.isEmpty) {
                Log.w(TAG, "Knowledge base is empty")
                return "Knowledge base is currently empty. Please try again later."
            }

            Log.d(TAG, "Found ${snapshot.size()} documents in knowledge base")

            val similarities = mutableListOf<Triple<String, Double, String>>()

            try {
                // ✅ Use coroutineScope to create proper context for async
                coroutineScope {
                    val embeddingJobs = snapshot.documents.map { doc ->
                        async(Dispatchers.Default) {
                            try {
                                val docId = doc.id
                                val docData = doc.data ?: return@async null
                                val docText = formatData(docData)

                                delay(50)

                                val docEmbedding = getEmbedding(docText.take(2000))
                                if (docEmbedding.isEmpty()) return@async null

                                val baseSim = cosineSimilarity(questionEmbedding, docEmbedding)

                                var boost = 0.0
                                val docLower = docText.lowercase()

                                when (analysis.intent) {
                                    QueryIntent.ROLE_QUERY -> {
                                        analysis.roleKeyword?.let { role ->
                                            if (docLower.contains(role) ||
                                                (role == "chair" && (docLower.contains("chairperson") || docLower.contains("department head")))) {
                                                boost = 0.6
                                                Log.d(TAG, "🎯 ROLE '$role' found in $docId")
                                            }
                                        }
                                    }
                                    QueryIntent.PERSON_QUERY -> {
                                        analysis.personName?.let { name ->
                                            if (fuzzyContains(docText, name)) {
                                                boost = 0.4
                                                Log.d(TAG, "🎯 PERSON '$name' found in $docId")
                                            }
                                        }
                                    }
                                    QueryIntent.GENERAL_QUERY -> {
                                        boost = 0.0
                                    }
                                }

                                val finalSim = (baseSim + boost).coerceAtMost(1.0)
                                Triple(docId, finalSim, docText)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error processing document: ${e.message}")
                                null
                            }
                        }
                    }

                    val results = withTimeoutOrNull(30000L) {
                        embeddingJobs.awaitAll()
                    }

                    if (results != null) {
                        for (result in results) {
                            if (result != null) {
                                similarities.add(result)
                                Log.d(TAG, "📊 ${result.first}: ${(result.second * 100).toInt()}%")
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during parallel embedding: ${e.message}", e)
                return "Error processing embeddings. Please try again."
            }

            if (similarities.isEmpty()) {
                Log.w(TAG, "No similar documents found")
                return "No relevant information found for your question. Try rephrasing it."
            }

            val topDocs = similarities.sortedByDescending { it.second }.take(3)
            Log.d(TAG, "🏆 Top results: ${topDocs.map { "${it.first}(${(it.second * 100).toInt()}%)" }}")

            val contextSnippets = mutableListOf<Triple<Double, String, String>>()

            when (analysis.intent) {
                QueryIntent.ROLE_QUERY -> {
                    analysis.roleKeyword?.let { role ->
                        for (doc in topDocs) {
                            val docId = doc.first
                            val sim = doc.second
                            val docText = doc.third
                            val section = extractRoleSection(docText, role)
                            if (section.isNotEmpty()) {
                                contextSnippets.add(Triple(sim, docId, section))
                            }
                        }
                    }
                }
                QueryIntent.PERSON_QUERY -> {
                    analysis.personName?.let { name ->
                        for (doc in topDocs) {
                            val docId = doc.first
                            val sim = doc.second
                            val docText = doc.third
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
                    for (doc in topDocs) {
                        val docId = doc.first
                        val sim = doc.second
                        val docText = doc.third
                        contextSnippets.add(Triple(sim, docId, docText.take(2000)))
                    }
                }
            }

            for (doc in topDocs) {
                val docId = doc.first
                val sim = doc.second
                val docText = doc.third
                val alreadyAdded = contextSnippets.any { it.second == docId }
                if (!alreadyAdded) {
                    contextSnippets.add(Triple(sim, docId, docText.take(1500)))
                }
            }

            val sortedSnippets = contextSnippets.sortedByDescending { it.first }

            if (sortedSnippets.isEmpty()) {
                Log.w(TAG, "No relevant snippets extracted")
                return "Unable to find relevant information. Try rewording your question."
            }

            val context = StringBuilder()
            for (snippet in sortedSnippets) {
                val sim = snippet.first
                val docId = snippet.second
                val text = snippet.third
                context.append("\n=== From $docId (Relevance: ${(sim * 100).toInt()}%) ===\n")
                context.append(text).append("\n")
            }

            val result = context.toString()
            val limited = if (result.length > 6000) result.take(6000) else result

            Log.d(TAG, "✅ Context retrieved: ${limited.length} chars")
            contextCache[cacheKey] = Pair(limited, System.currentTimeMillis())
            limited

        } catch (e: Exception) {
            Log.e(TAG, "Critical error in getRelevantContext: ${e.message}", e)
            "System error occurred. Please try again shortly."
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

    private fun buildSystemPrompt(
        context: String,
        analysis: QueryAnalysis,
        studentProfile: String
    ): String {
        val baseInstructions = """
        You are Morgan State University's AI assistant.
        
        STUDENT ACADEMIC PROFILE (if available):
        ${if (studentProfile.isBlank()) "No DegreeWorks profile is available for this user yet." else studentProfile}
        
        CRITICAL FORMATTING RULES:
        - DO NOT use asterisks (*) or any markdown formatting
        - DO NOT use bold, italic, or any special formatting
        - Write in plain, natural text only
        - Use simple punctuation: periods, commas, colons, parentheses only
        
        RESPONSE INSTRUCTIONS:
        - Answer ONLY using information from the knowledge base and the student profile below
        - The knowledge base is ordered by relevance - prioritize the first documents
        - Extract exact details: names, emails, phones, dates, URLs
        - When the question is about the student's situation (GPA, courses, credits, remaining requirements),
          use the STUDENT ACADEMIC PROFILE section above
        - Be conversational and helpful
        - If exact information isn't found, provide the closest related information
        - Try your best to be helpful - don't say "I don't have that information"
        
        WHAT NOT TO DO:
        - Don't make up information
        - Don't confuse different people
        - Don't use asterisks or formatting symbols
        - Don't apologize for limitations
    """.trimIndent()

        val specificInstructions = when (analysis.intent) {
            QueryIntent.ROLE_QUERY -> {
                """
            
            ROLE QUERY - Find: ${analysis.roleKeyword}
            - Look for the specific role mentioned
            - Find the person who holds this position
            - Provide their name, title, email, phone, office location
            - Write in natural sentences like you're talking to a friend
            """.trimIndent()
            }
            QueryIntent.PERSON_QUERY -> {
                """
            
            PERSON QUERY - Find information about: ${analysis.personName}
            - Locate details about this specific person
            - Provide their title, role, contact information
            - Write in natural, conversational sentences
            - Be accurate and don't confuse with other faculty
            """.trimIndent()
            }
            QueryIntent.GENERAL_QUERY -> {
                """
            
            GENERAL QUERY
            - Provide the most relevant and helpful information available
            - Answer the user's question as directly as possible
            - Guide them to relevant resources if needed
            - Use plain text, no formatting
            """.trimIndent()
            }
        }

        return """
        $baseInstructions
        $specificInstructions
        
        KNOWLEDGE BASE (ordered by relevance):
        $context
        
        Answer the user's question accurately and naturally. Use PLAIN TEXT ONLY - absolutely NO asterisks, NO markdown, NO special formatting.
    """.trimIndent()
    }


    suspend fun getChatResponse(userMessage: String): String {
        return try {
            Log.d(TAG, "Processing message: ${userMessage.take(50)}")

            handleCasualConversation(userMessage)?.let {
                Log.d(TAG, "Casual response matched")
                return it
            }

            val analysis = analyzeQuery(userMessage)
            val context = getRelevantContext(userMessage, analysis)

// NEW: add DegreeWorks summary
            val studentProfile = getStudentProfileSummary()

            if (context.contains("Error") || context.contains("error") || context.contains("Unable")) {
                Log.w(TAG, "Context retrieval failed or returned error")
                return context
            }

// UPDATED: pass studentProfile into prompt
            val systemPrompt = buildSystemPrompt(context, analysis, studentProfile)


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

            withContext(Dispatchers.IO) {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful) {
                    Log.e(TAG, "OpenAI API error: ${response.code} - ${response.message}")
                    return@withContext "I'm temporarily unable to process that. Please try again in a moment."
                }

                try {
                    val json = JSONObject(responseBody ?: "")
                    val message = json.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")

                    Log.d(TAG, "✅ Response generated successfully")
                    message.trim()
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing API response: ${e.message}")
                    "I understood your question but had trouble formulating a response. Please try again."
                }
            }

        } catch (e: IOException) {
            Log.e(TAG, "Network error: ${e.message}")
            "Network connection issue. Please check your internet and try again."
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}", e)
            "Something went wrong. Please try your question again."
        }
    }
}
