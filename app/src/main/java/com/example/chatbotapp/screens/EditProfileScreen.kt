package com.example.chatbotapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.activity.compose.BackHandler
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Public

import com.example.chatbotapp.screens.DegreeWorksActivity




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val user = auth.currentUser

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var password by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current


    // Load the user's current profile name from Firestore
    LaunchedEffect(user?.uid) {
        user?.uid?.let { uid ->
            db.collection("users").document(uid).get()
                .addOnSuccessListener { snapshot ->
                    fullName = snapshot.getString("fullName") ?: ""
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.navigate("main?selectedTab=2") {
                                popUpTo("main") { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Update your account details below.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("New Password (optional)") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )

            if (message != null) {
                Text(
                    text = message!!,
                    color = if (message!!.contains("success", ignoreCase = true))
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Button(
                onClick = {
                    if (user != null) {
                        isSaving = true
                        scope.launch {
                            try {
                                // Update name in Firestore
                                db.collection("users").document(user.uid)
                                    .update("fullName", fullName)

                                // Update email
                                if (email.isNotEmpty() && email != user.email) {
                                    user.updateEmail(email).await()
                                }

                                // Update password if provided
                                if (password.isNotEmpty()) {
                                    user.updatePassword(password).await()
                                }

                                message = "Profile updated successfully!"
                            } catch (e: Exception) {
                                message = "Error: ${e.localizedMessage}"
                            } finally {
                                isSaving = false
                            }
                        }
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save Changes")
                }
            }
            //Update DegreeWorks Button
            OutlinedButton(
                onClick = {
                    val url = "https://morgan.edu/gateway"
                    val intent = Intent(context, DegreeWorksActivity::class.java)
                    intent.putExtra("url", url)
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(0.95f)
            ) {
                Icon(Icons.Filled.Public, contentDescription = "Launch DegreeWorks Gateway")
                Spacer(Modifier.width(8.dp))
                Text("Update DegreeWorks")
            }
        }
    }
}



