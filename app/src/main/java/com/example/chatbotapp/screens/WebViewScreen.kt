package com.example.chatbotapp.screens

import android.content.Intent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WebViewScreen(
    url: String = "https://www.morgan.edu/gateway/",
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Launch the DegreeWorksActivity once
    LaunchedEffect(Unit) {
        val intent = Intent(context, DegreeWorksActivity::class.java)
        intent.putExtra("url", url)
        context.startActivity(intent)
        onClose()
    }

    //simple loading UI while launching
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text("Opening DegreeWorks Portal…")
    }
}
