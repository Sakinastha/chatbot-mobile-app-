// File: app/src/main/java/com/example/chatbotapp/screens/OtherScreens.kt
@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.chatbotapp.screens

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.animateContentSize
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Help
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chatbotapp.auth.AuthService
import com.example.chatbotapp.auth.UserProfile
import android.widget.Button
import kotlinx.coroutines.launch

// Data classes for MSU courses
data class MSUCourse(
    val courseCode: String,
    val courseName: String,
    val credits: Int,
    val prerequisites: List<String>,
    val offered: List<String>,
    val category: String = "Core",
    val isTaken: Boolean = false
)

@Composable
fun CurriculumContent(modifier: Modifier = Modifier,
                      onLaunchBrowser: (String) -> Unit) {
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var expandedCourse by remember { mutableStateOf<String?>(null) }
    // Loading state
    var isLoading by remember { mutableStateOf(true) }
    // Holds taken course codes loaded from Firestore
    var takenCourses by remember { mutableStateOf<List<String>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    var hasDegreeworksData by remember { mutableStateOf(false) }


    val categories = listOf("All", "Math", "Core CS", "Advanced", "Electives", "Capstone","Taken")

    // Fetch taken courses from Firestore once
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    val snapshot = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userId)
                        .collection("degreeworks")
                        .document("latest")
                        .get()
                        .await()

                    if (snapshot.exists() && snapshot.data?.isNotEmpty() == true) {
                        hasDegreeworksData = true
                        val sections = snapshot.get("sections") as? List<Map<String, Any>>
                        val completed = mutableListOf<String>()
                        //sort taken classes from firestore
                        sections?.forEach { section ->
                            val completedCourses =
                                section["completed_courses"] as? List<Map<String, Any>>
                            completedCourses?.forEach { course ->
                                val code = (course["course"] as? String)?.trim()
                                val termText = (course["term"] as? String)?.trim().orEmpty()

                                // Always add the main course code if available
                                if (!code.isNullOrEmpty()) completed.add(code)

                                // Also extract any hidden courses like COSC 112, COSC 220, etc.
                                val embeddedCourses = Regex("""\b[A-Z]{2,4}\s?\d{3}\b""")
                                    .findAll(termText)
                                    .map { it.value.trim() }
                                    .toList()

                                completed.addAll(embeddedCourses)
                            }
                        }
                        takenCourses = completed
                    } }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }


    val msuCourses = remember {
        listOf(
            // Math Foundation
            MSUCourse("MATH 241", "Calculus I", 4, listOf("ENGR 101", "MATH 114", "MATH 141"), listOf("Fall", "Spring"), "Math"),
            MSUCourse("MATH 242", "Calculus II", 4, listOf("MATH 241 (Grade C or higher)"), listOf("Fall", "Spring"), "Math"),
            MSUCourse("MATH 312", "Linear Algebra I", 3, listOf("MATH 241 (Grade C or higher)"), listOf("Fall", "Spring"), "Math"),
            MSUCourse("MATH 331", "Applied Probability and Statistics", 3, listOf("MATH 242 (Grade C or higher)"), listOf("Fall", "Spring"), "Math"),

            // Core CS Courses
            MSUCourse("COSC 111", "Introduction to Computer Science I", 4, emptyList(), listOf("Fall", "Spring"), "Core CS"),
            MSUCourse("COSC 112", "Introduction to Computer Science II", 4, listOf("COSC 111 (Grade C or higher)"), listOf("Fall", "Spring"), "Core CS"),
            MSUCourse("COSC 220", "Data Structures and Algorithms Analysis", 4, listOf("COSC 112 (Grade C or higher)"), listOf("Fall", "Spring"), "Core CS"),
            MSUCourse("COSC 238", "Object Oriented Programming", 4, listOf("COSC 112 (Grade C or higher)"), listOf("Fall", "Spring"), "Core CS"),
            MSUCourse("COSC 241", "Computer Systems & Digital Logic", 3, listOf("COSC 112", "MATH 141 (Grade C or higher)"), listOf("Fall", "Spring"), "Core CS"),
            MSUCourse("COSC 281", "Discrete Structures", 3, listOf("COSC 112 (Grade C or higher)"), listOf("Fall", "Spring"), "Core CS"),

            // Advanced Courses
            MSUCourse("COSC 320", "Algorithm Design and Analysis", 3, listOf("COSC 220 (Grade C or higher)", "COSC 281 (Grade C or higher)"), listOf("Fall", "Spring"), "Advanced"),
            MSUCourse("COSC 354", "Operating Systems", 3, listOf("COSC 220 (Grade C or higher)", "COSC 241 (Grade C or higher)"), listOf("Fall", "Spring"), "Advanced"),
            MSUCourse("COSC 352", "Organization of Programming Languages", 3, listOf("COSC 220 (Grade C or higher)"), listOf("Fall", "Spring"), "Advanced"),
            MSUCourse("COSC 459", "Database Design", 3, listOf("COSC 220 (Grade C or higher)"), listOf("Fall", "Spring"), "Advanced"),

            // Specialized Electives
            MSUCourse("COSC 470", "Artificial Intelligence", 3, listOf("COSC 220 (Grade C or higher)"), listOf("Fall", "Spring"), "Electives"),
            MSUCourse("COSC 472", "Intro to Machine Learning", 3, listOf("COSC 112 (Grade C or higher)", "MATH 312 (Grade C or higher)"), listOf("Fall", "Spring"), "Electives"),
            MSUCourse("COSC 338", "Mobile App Design and Development", 3, listOf("COSC 238 (Grade C or higher)"), listOf("Fall", "Spring"), "Electives"),
            MSUCourse("COSC 351", "Foundations of Computer Security", 3, listOf("COSC 112 (Grade C or higher)"), listOf("Fall", "Spring"), "Electives"),
            MSUCourse("COSC 460", "Computer Graphics", 3, listOf("COSC 220 (Grade C or higher)", "MATH 241 (Grade C or higher)"), listOf("Fall", "Spring"), "Electives"),
            MSUCourse("COSC 332", "Introduction to Game Design and Development", 3, listOf("COSC 112 (Grade C or higher)"), listOf("Fall", "Spring"), "Electives"),
            MSUCourse("CLCO 261", "Intro to Cloud Computing", 3, listOf("COSC 112 (Grade C or higher)"), listOf("Fall", "Spring"), "Electives"),

            // Capstone
            MSUCourse("COSC 490", "Senior Project", 3, listOf("Department Chair permission", "Senior standing required"), listOf("As Needed"), "Capstone"),
            MSUCourse("COSC 001", "CS Senior Comprehensive Exam", 0, emptyList(), listOf("As Needed"), "Capstone")
        )
    }
    // Update courses with taken status from Firestore
    val displayedCourses = msuCourses.map { course ->
        course.copy(
            isTaken = takenCourses.any { it.contains(course.courseCode, ignoreCase = true) }
        )
    }

    val filteredCourses = displayedCourses.filter { course ->
        val matchesCategory = when (selectedCategory) {
            "All" -> true
            "Taken" -> course.isTaken
            else -> course.category == selectedCategory
        }
        val matchesSearch = searchQuery.isEmpty() ||
                course.courseCode.contains(searchQuery, ignoreCase = true) ||
                course.courseName.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }
    // Loading overlay
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "MSU Computer Science",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Course Curriculum & Prerequisites",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "${filteredCourses.size} Courses",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search courses...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Category filter
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { category ->
                FilterChip(
                    onClick = { selectedCategory = category },
                    label = { Text(category) },
                    selected = selectedCategory == category,
                    leadingIcon = if (selectedCategory == category) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        // Degreeworks button (Only shows if no degreeworks info is found)
        if (!hasDegreeworksData) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        val url = "https://morgan.edu/gateway"
                        onLaunchBrowser(url)
                    },
                    modifier = Modifier.fillMaxWidth(0.95f)
                ) {
                    Icon(Icons.Filled.Public, contentDescription = "Launch Degreeworks Gateway")
                    Spacer(Modifier.width(8.dp))
                    Text("Access Degreeworks Gateway")
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sign in, navigate to your Degreeworks page.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }


        // Course list
        if (selectedCategory == "Taken" && filteredCourses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 40.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = "No classes taken yet or DegreeWorks not uploaded.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                state = rememberLazyListState()
            ) {
                items(filteredCourses) { course ->
                    MSUCourseCard(
                        course = course,
                        isExpanded = expandedCourse == course.courseCode,
                        onToggleExpand = {
                            expandedCourse = if (expandedCourse == course.courseCode) null else course.courseCode
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MSUCourseCard(
    course: MSUCourse,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onToggleExpand
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Course header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = course.courseCode,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = getCategoryColor(course.category),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = "${course.credits} cr",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Text(
                        text = course.courseName,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Icon(
                    if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))

                // Prerequisites
                if (course.prerequisites.isNotEmpty()) {
                    Text(
                        text = "Prerequisites:",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    course.prerequisites.forEach { prereq ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Filled.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = prereq,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    Text(
                        text = "Prerequisites: None",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Offered terms
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Offered: ",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    course.offered.forEachIndexed { index, term ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = term,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        if (index < course.offered.size - 1) {
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun getCategoryColor(category: String): androidx.compose.ui.graphics.Color {
    return when (category) {
        "Math" -> MaterialTheme.colorScheme.tertiary
        "Core CS" -> MaterialTheme.colorScheme.primary
        "Advanced" -> MaterialTheme.colorScheme.secondary
        "Electives" -> MaterialTheme.colorScheme.error
        "Capstone" -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
}

@Composable
fun ProfileContent(
    modifier: Modifier = Modifier,
    onEditProfile: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val authService = remember { AuthService() }
    val scope = rememberCoroutineScope()

    // Load user profile
    LaunchedEffect(Unit) {
        userProfile = authService.getUserProfile()
        isLoading = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isLoading) {
            // Loading state
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Profile header card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile avatar
                    Card(
                        modifier = Modifier.size(80.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                Icons.Filled.AccountCircle,
                                contentDescription = "Profile",
                                modifier = Modifier.size(50.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // User name
                    Text(
                        text = userProfile?.fullName ?: "User",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )

                    // User email
                    Text(
                        text = userProfile?.email ?: "user@example.com",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // User ID badge
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "Member since ${java.text.SimpleDateFormat("MMM yyyy").format(java.util.Date())}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Settings items
            val settingsItems = listOf(
                SettingsItem("Edit Profile", Icons.Filled.Edit),
                SettingsItem("Notifications", Icons.Filled.Notifications),
                SettingsItem("Privacy & Security", Icons.Filled.Security),
                SettingsItem("Help & Support", Icons.Filled.Help),
                SettingsItem("About", Icons.Filled.Info)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(settingsItems) { settingsItem ->
                    SettingsCard(
                        title = settingsItem.title,
                        icon = settingsItem.icon,
                        onClick = { when (settingsItem.title) {
                            "Edit Profile" -> {onEditProfile()}
                        }

                            // Handle settings item clicks
                        }
                    )
                }

                // Logout item - separate and styled differently
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsCard(
                        title = "Sign Out",
                        icon = Icons.Filled.ExitToApp,
                        onClick = { showLogoutDialog = true },
                        isDestructive = true
                    )
                }
            }
        }
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out of your account?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        authService.signOut()
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Sign Out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Data class for settings items
data class SettingsItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit = {},
    isDestructive: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDestructive) 0.dp else 2.dp),
        colors = if (isDestructive) CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        ) else CardDefaults.cardColors(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = if (isDestructive)
                    MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                color = if (isDestructive)
                    MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface
            )
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}