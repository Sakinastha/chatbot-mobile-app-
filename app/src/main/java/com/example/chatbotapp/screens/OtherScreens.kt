// File: app/src/main/java/com/example/chatbotapp/screens/OtherScreens.kt
@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.chatbotapp.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.example.chatbotapp.auth.AuthService
import com.example.chatbotapp.auth.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
fun AboutScreen(onBack: () -> Unit = {}) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Back Button & Title
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onBack() }) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "About",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // App Icon & Name Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(6.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "MSU AI Assistant",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Version 1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Divider()
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "An intelligent AI-powered mobile assistant designed exclusively for Morgan State University students. Get instant answers to academic questions, explore curriculum information, receive personalized degree guidance, and access comprehensive university resources—all through an intuitive mobile interface powered by advanced AI technology.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Features Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        "Key Features",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))
                    FeatureItem(Icons.Filled.Chat, "AI-Powered Conversations", "Get instant, intelligent responses to your academic questions")
                    FeatureItem(Icons.Filled.School, "Curriculum Guidance", "Access detailed information about courses, majors, and degree requirements")
                    FeatureItem(Icons.Filled.Person, "Personalized Assistance", "Receive tailored recommendations based on your academic profile")
                    FeatureItem(Icons.Filled.Security, "Secure & Private", "Your conversations and data are encrypted and protected")
                    FeatureItem(Icons.Filled.CloudDone, "Cloud Sync", "Access your chat history across devices seamlessly")
                }
            }

            // Technology Stack Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        "Built With",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))
                    TechItem("Kotlin & Jetpack Compose", "Modern Android development")
                    TechItem("OpenAI GPT", "Advanced AI language model")
                    TechItem("Firebase", "Real-time database and authentication")
                    TechItem("FastAPI Backend", "High-performance Python API")
                }
            }

            // Contact & Support Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Contact Us",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "For questions, feedback, or technical support:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "sashr5@morgan.edu",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "aashr3@morgan.edu",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "© 2025 Morgan State University",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Developed by MSU Computer Science Department",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun FeatureItem(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TechItem(title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                )
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PrivacySecurityScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy & Security") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.Security,
                        contentDescription = "Security",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Your Privacy Matters",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "We are committed to protecting your personal information and maintaining transparency about our data practices.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Data Collection Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    SectionHeader(Icons.Filled.Dataset, "Data We Collect")
                    Spacer(Modifier.height(12.dp))
                    PrivacyPoint("Account Information", "Email, name, and authentication credentials for account creation and login")
                    PrivacyPoint("Chat History", "Your conversations with the AI assistant to provide personalized responses")
                    PrivacyPoint("Usage Analytics", "Anonymous usage data to improve app performance and user experience")
                    PrivacyPoint("Device Information", "Basic device data for compatibility and troubleshooting purposes")
                }
            }

            // How We Use Data Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    SectionHeader(Icons.Filled.Policy, "How We Use Your Data")
                    Spacer(Modifier.height(12.dp))
                    PrivacyPoint("Personalized Experience", "Tailor AI responses based on your academic profile and history")
                    PrivacyPoint("Service Improvement", "Analyze usage patterns to enhance features and performance")
                    PrivacyPoint("Security & Support", "Maintain account security and provide technical assistance")
                    PrivacyPoint("Communication", "Send important updates about the app and your account")
                }
            }

            // Data Protection Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    SectionHeader(Icons.Filled.Shield, "Data Protection")
                    Spacer(Modifier.height(12.dp))
                    PrivacyPoint("Encryption", "All data transmitted between your device and servers is encrypted using industry-standard TLS protocols")
                    PrivacyPoint("Secure Storage", "Data is stored on secure cloud servers with restricted access and regular security audits")
                    PrivacyPoint("Firebase Security", "Leveraging Google Firebase's enterprise-grade security infrastructure")
                    PrivacyPoint("No Third-Party Sharing", "We never sell or share your personal data with third parties for marketing purposes")
                }
            }

            // Your Rights Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    SectionHeader(Icons.Filled.VerifiedUser, "Your Privacy Rights")
                    Spacer(Modifier.height(12.dp))
                    PrivacyPoint("Access Your Data", "Request a copy of all personal data we have stored about you")
                    PrivacyPoint("Delete Your Data", "Request permanent deletion of your account and associated data")
                    PrivacyPoint("Opt-Out Options", "Control what data is collected and how it's used")
                    PrivacyPoint("Data Portability", "Export your chat history and account information at any time")
                }
            }

            // Compliance Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Compliance & Standards",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "This app complies with:\n• FERPA (Family Educational Rights and Privacy Act)\n• COPPA (Children's Online Privacy Protection Act)\n• GDPR principles for data protection\n• University data security policies",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Last Updated: November 2025",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "For detailed privacy information, contact:\nprivacy@morgan.edu",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun PrivacyPoint(title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(6.dp)
                .background(
                    MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                )
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun HelpSupportScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help & Support") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Welcome Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.Help,
                        contentDescription = "Help",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "How Can We Help?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Find answers to common questions or reach out to our support team for personalized assistance.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Getting Started Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    SectionHeader(Icons.Filled.RocketLaunch, "Getting Started")
                    Spacer(Modifier.height(12.dp))
                    FAQItem(
                        "How do I create an account?",
                        "Tap 'Sign Up' on the login screen, enter your Morgan State email address, create a secure password, and verify your email to get started."
                    )
                    FAQItem(
                        "How do I reset my password?",
                        "On the login screen, tap 'Forgot Password?', enter your registered email, and follow the password reset link sent to your inbox."
                    )
                    FAQItem(
                        "What can the AI assistant help me with?",
                        "Ask about course requirements, degree programs, academic policies, campus resources, class schedules, and general university information."
                    )
                }
            }

            // Account & Settings Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    SectionHeader(Icons.Filled.Settings, "Account & Settings")
                    Spacer(Modifier.height(12.dp))
                    FAQItem(
                        "How do I update my profile?",
                        "Go to Settings → Profile, where you can update your name, email, major, and graduation year."
                    )
                    FAQItem(
                        "Can I delete my account?",
                        "Yes. Go to Settings → Account → Delete Account. Note: This permanently removes all your data and cannot be undone."
                    )
                    FAQItem(
                        "How do I change notification preferences?",
                        "Navigate to Settings → Notifications to customize which alerts you receive and how you're notified."
                    )
                }
            }

            // Chat & Features Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    SectionHeader(Icons.Filled.Chat, "Chat & Features")
                    Spacer(Modifier.height(12.dp))
                    FAQItem(
                        "How do I clear my chat history?",
                        "Open the chat screen, tap the menu icon (⋮), and select 'Clear Chat History'. You can also manage individual conversations."
                    )
                    FAQItem(
                        "Does the app work offline?",
                        "Limited functionality is available offline. You can view previous chats, but active internet is required for new AI responses."
                    )
                    FAQItem(
                        "Can I save important conversations?",
                        "Yes! Tap the bookmark icon on any chat to save it for quick access later from your Saved Chats section."
                    )
                }
            }

            // Troubleshooting Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    SectionHeader(Icons.Filled.Build, "Troubleshooting")
                    Spacer(Modifier.height(12.dp))
                    FAQItem(
                        "The app is running slowly",
                        "Try clearing the app cache in Settings → Storage. Ensure you have a stable internet connection and the latest app version."
                    )
                    FAQItem(
                        "I'm not receiving responses",
                        "Check your internet connection. If the issue persists, try logging out and back in, or restart the app."
                    )
                    FAQItem(
                        "Login issues",
                        "Verify your email and password are correct. Clear app data or reinstall if problems continue. Contact support if needed."
                    )
                }
            }

            // Contact Support Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.ContactSupport,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Still Need Help?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Our support team is here to assist you",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))

                    // Email Support
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "Email Support",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "sashr5@morgan.edu",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "aashr3@morgan.edu",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Divider()
                    Spacer(Modifier.height(16.dp))

                    // Response Time
                    Text(
                        "📧 Response Time: 24-48 hours",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "🕒 Support Hours: Monday-Friday, 9 AM - 5 PM EST",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Additional Resources Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        "Additional Resources",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))
                    ResourceLink(Icons.Filled.School, "Morgan State University Website", "www.morgan.edu")
                    ResourceLink(Icons.Filled.MenuBook, "Student Handbook", "Access university policies and guidelines")
                    ResourceLink(Icons.Filled.CalendarToday, "Academic Calendar", "View important dates and deadlines")
                    ResourceLink(Icons.Filled.Computer, "IT Support", "Technical assistance for university systems")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FAQItem(question: String, answer: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            question,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            answer,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ResourceLink(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CurriculumContent(
    modifier: Modifier = Modifier,
    onLaunchBrowser: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var expandedCourse by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var takenCourses by remember { mutableStateOf<List<String>>(emptyList()) }
    var takenCoursesDetails by remember { mutableStateOf<List<CompletedCourse>>(emptyList()) }
    var hasDegreeworksData by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val categories = listOf("All", "Math", "Core CS", "Advanced", "Electives", "Capstone", "Taken")

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
                        val sections = snapshot.get("json_preview.sections") as? List<Map<String, Any>>
                        val completed = mutableListOf<String>()
                        val detailedCoursesMap = mutableMapOf<String, CompletedCourse>() // ✅ USE MAP

                        sections?.forEach { section ->
                            val completedCourses = section["completed_courses"] as? List<Map<String, Any>>
                            completedCourses?.forEach { course ->
                                val code = (course["course"] as? String)?.trim()
                                val title = (course["title"] as? String)?.trim() ?: ""
                                val grade = (course["grade"] as? String)?.trim() ?: ""
                                val credits = (course["credits"] as? String)?.trim() ?: ""

                                if (!code.isNullOrEmpty() && grade != "IP") {
                                    completed.add(code)

                                    // ✅ ONLY ADD IF NOT ALREADY IN MAP (prevents duplicates)
                                    if (!detailedCoursesMap.containsKey(code)) {
                                        detailedCoursesMap[code] = CompletedCourse(
                                            code = code,
                                            title = title,
                                            grade = grade,
                                            credits = credits
                                        )
                                    }
                                }
                            }
                        }

                        takenCourses = completed.distinct() // ✅ REMOVE DUPLICATES
                        takenCoursesDetails = detailedCoursesMap.values.toList().sortedBy { it.code } // ✅ CONVERT MAP TO LIST
                    }
                }
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
            MSUCourse("MATH 241", "Calculus I", 4, listOf("ENGR 101, MATH 114, MATH 141"), listOf("Fall", "Spring"), "Math"),
            MSUCourse("MATH 242", "Calculus II", 4, listOf("MATH 241 (Grade C or higher)"), listOf("Fall", "Spring"), "Math"),
            MSUCourse("MATH 312", "Linear Algebra I", 3, listOf("MATH 241 (Grade C or higher)"), listOf("Fall", "Spring"), "Math"),
            MSUCourse("MATH 331", "Applied Probability and Statistics", 3, listOf("MATH 242 (Grade C or higher)"), listOf("Fall", "Spring"), "Math"),

            // Core CS Courses
            MSUCourse("COSC 111", "Introduction to Computer Science I", 4, emptyList(), listOf("Fall", "Spring"), "Core CS"),
            MSUCourse("COSC 112", "Introduction to Computer Science II", 4, listOf("COSC 111 (Grade C or higher)"), listOf("Fall", "Spring"), "Core CS"),
            MSUCourse("COSC 220", "Data Structures and Algorithms Analysis", 4, listOf("COSC 112 (Grade C or higher)"), listOf("Fall", "Spring"), "Core CS"),
            MSUCourse("COSC 238", "Object Oriented Programming", 4, listOf("COSC 112 (Grade C or higher)"), listOf("Fall", "Spring"), "Core CS"),
            MSUCourse("COSC 241", "Computer Systems & Digital Logic", 3, listOf("COSC 112, MATH 141 (Grade C or higher)"), listOf("Fall", "Spring"), "Core CS"),
            MSUCourse("COSC 281", "Discrete Structures", 3, listOf("COSC 112 (Grade C or higher)"), listOf("Fall", "Spring"), "Core CS"),

            // Advanced Courses
            MSUCourse("COSC 320", "Algorithm Design and Analysis", 3, listOf("COSC 220, COSC 281 (Grade C or higher)"), listOf("Fall", "Spring"), "Advanced"),
            MSUCourse("COSC 354", "Operating Systems", 3, listOf("COSC 220, COSC 241 (Grade C or higher)"), listOf("Fall", "Spring"), "Advanced"),
            MSUCourse("COSC 352", "Organization of Programming Languages", 3, listOf("COSC 220 (Grade C or higher)"), listOf("Fall", "Spring"), "Advanced"),
            MSUCourse("COSC 459", "Database Design", 3, listOf("COSC 220 (Grade C or higher)"), listOf("Fall", "Spring"), "Advanced"),

            // Specialized Electives
            MSUCourse("COSC 470", "Artificial Intelligence", 3, listOf("COSC 220 (Grade C or higher)"), listOf("Fall", "Spring"), "Electives"),
            MSUCourse("COSC 472", "Intro to Machine Learning", 3, listOf("COSC 112, MATH 312 (Grade C or higher)"), listOf("Fall", "Spring"), "Electives"),
            MSUCourse("COSC 338", "Mobile App Design and Development", 3, listOf("COSC 238 (Grade C or higher)"), listOf("Fall", "Spring"), "Electives"),
            MSUCourse("COSC 351", "Foundations of Computer Security", 3, listOf("COSC 112 (Grade C or higher)"), listOf("Fall", "Spring"), "Electives"),
            MSUCourse("COSC 460", "Computer Graphics", 3, listOf("COSC 220, MATH 241 (Grade C or higher)"), listOf("Fall", "Spring"), "Electives"),
            MSUCourse("COSC 332", "Introduction to Game Design and Development", 3, listOf("COSC 112 (Grade C or higher)"), listOf("Fall", "Spring"), "Electives"),
            MSUCourse("CLCO 261", "Intro to Cloud Computing", 3, listOf("COSC 112 (Grade C or higher)"), listOf("Fall", "Spring"), "Electives"),

            // Capstone
            MSUCourse("COSC 490", "Senior Project", 3, listOf("Department Chair permission", "Senior standing required"), listOf("As Needed"), "Capstone"),
            MSUCourse("COSC 001", "CS Senior Comprehensive Exam", 0, emptyList(), listOf("As Needed"), "Capstone")
        )
    }

    val displayedCourses = msuCourses.map { course ->
        course.copy(isTaken = takenCourses.any { it.contains(course.courseCode, ignoreCase = true) })
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

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
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

        // Degreeworks button
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
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Course list - SPECIAL HANDLING FOR "TAKEN" TAB
        if (selectedCategory == "Taken") {
            if (takenCoursesDetails.isEmpty()) {
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
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    state = rememberLazyListState()
                ) {
                    items(takenCoursesDetails.filter { course ->
                        searchQuery.isEmpty() ||
                                course.code.contains(searchQuery, ignoreCase = true) ||
                                course.title.contains(searchQuery, ignoreCase = true)
                    }) { course ->
                        TakenCourseCard(course)
                    }
                }
            }
        } else {
            // Regular course display for other tabs
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

// Data class for completed courses with details
data class CompletedCourse(
    val code: String,
    val title: String,
    val grade: String,
    val credits: String
)

@Composable
fun TakenCourseCard(course: CompletedCourse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Checkmark icon
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Completed",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )

                Column {
                    Text(
                        text = course.code,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = course.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }

            // Grade and Credits
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = getGradeColor(course.grade).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = course.grade,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = getGradeColor(course.grade)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${course.credits} cr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun getGradeColor(grade: String): Color {
    return when (grade.uppercase()) {
        "A", "A-" -> Color(0xFF4CAF50)
        "B+", "B", "B-" -> Color(0xFF2196F3)
        "C+", "C", "C-" -> Color(0xFFFF9800)
        "TRA", "TRB" -> Color(0xFF9C27B0) // Transfer credits
        else -> Color(0xFF757575)
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
    onLogout: () -> Unit = {},
    onAbout: () -> Unit = {},
    onPrivacySecurity: () -> Unit = {},
    onHelpSupport: () -> Unit = {},
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
                            text = "Member since ${ java.text.SimpleDateFormat("MMM yyyy").format(java.util.Date()) }",
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
                        onClick = {
                            when (settingsItem.title) {
                                "Edit Profile" -> onEditProfile()
                                "About" -> onAbout()
                                "Privacy & Security" -> onPrivacySecurity()
                                "Help & Support" -> onHelpSupport()
                            }
                        }
                    )
                }

                // Logout item
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
    val icon: ImageVector
)

@Composable
fun SettingsCard(
    title: String,
    icon: ImageVector,
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
