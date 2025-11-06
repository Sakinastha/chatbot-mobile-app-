// File: app/src/main/java/com/example/chatbotapp/screens/WelcomeScreen.kt
@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.chatbotapp.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.chatbotapp.R
import androidx.compose.ui.BiasAlignment

@Composable
fun WelcomeScreen(navController: NavController) {
    var showTermsDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Full-screen background image
        Image(
            painter = painterResource(id = R.drawable.morganstate),
            contentDescription = "Morgan State University Campus",
            modifier = Modifier
                .fillMaxSize()
                .blur(1.dp),
            contentScale = ContentScale.Crop,
            alignment = BiasAlignment(0.4f, 0f)
        )

        // Dark gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Content layer with dynamic positioning
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // TOP SECTION - Left aligned
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 100.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Welcome to",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "MSU ChatBot",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MorganOrange,
                        letterSpacing = 0.8.sp
                    )
                )
            }

            // MIDDLE SECTION - Centered
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "A small step towards making\nyour college journey",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "10× easier, smarter,\nand faster",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MorganOrange,
                        textAlign = TextAlign.Center,
                        lineHeight = 30.sp
                    )
                )
            }

            // BOTTOM SECTION - Centered with subtitle
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Subtitle - Center aligned
                Text(
                    text = "Your intelligent AI companion for navigating academics, resources, and campus life at Morgan State University",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                )

                // Get Started Button - Now shows terms first
                Button(
                    onClick = { showTermsDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(30.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MorganOrange,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 12.dp,
                        pressedElevation = 16.dp
                    )
                ) {
                    Text(
                        "Get Started",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            letterSpacing = 0.5.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Terms & Conditions Button - Also shows terms
                TextButton(
                    onClick = { showTermsDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Terms & conditions",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 15.sp
                        )
                    )
                }
            }
        }
    }

    // Terms & Conditions Dialog
    if (showTermsDialog) {
        TermsAndConditionsDialog(
            onDismiss = { showTermsDialog = false },
            onAccept = {
                showTermsDialog = false
                navController.navigate("login")
            }
        )
    }
}

@Composable
fun TermsAndConditionsDialog(
    onDismiss: () -> Unit,
    onAccept: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = DarkSurface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Terms",
                        tint = MorganOrange,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Terms & Conditions",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Welcome to MSU ChatBot",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MorganOrange
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = """
                            By using this application, you agree to the following terms and conditions:

                            1. Acceptance of Terms
                            By accessing and using MSU ChatBot, you accept and agree to be bound by these Terms and Conditions.

                            2. User Responsibilities
                            • You must provide accurate information when using the chatbot
                            • You are responsible for maintaining the confidentiality of your account
                            • You agree to use the service for lawful purposes only

                            3. Service Description
                            MSU ChatBot provides AI-powered assistance for:
                            • Academic information and guidance
                            • Campus resources and facilities
                            • Course information and curriculum details
                            • General university inquiries

                            4. Data Usage & Privacy
                            • We collect and store conversation data to improve our service
                            • Your personal information is protected and never shared without consent
                            • Chat logs may be reviewed for quality assurance

                            5. Accuracy of Information
                            • While we strive for accuracy, information provided should be verified
                            • The chatbot is a supplementary tool, not a replacement for official university resources

                            6. Limitations of Liability
                            Morgan State University is not liable for any decisions made based solely on chatbot interactions.

                            7. Changes to Terms
                            We reserve the right to modify these terms at any time. Continued use constitutes acceptance of changes.

                            8. Contact
                            For questions about these terms, contact the Computer Science Department at Morgan State University.

                            Last Updated: October 2025
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = LightText.copy(alpha = 0.9f),
                            lineHeight = 20.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Decline Button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = LightText
                        )
                    ) {
                        Text(
                            "Decline",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    // Accept Button
                    Button(
                        onClick = onAccept,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MorganOrange,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            "Accept",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}
