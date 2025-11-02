package com.example.chatbotapp.screens

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.activity.ComponentActivity
import com.example.chatbotapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class DegreeWorksActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var confirmButton: Button
    private var loadingDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_degreeworks)

        webView = findViewById(R.id.webView)
        confirmButton = findViewById(R.id.confirmButton)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        // Fade effect setup
        webView.alpha = 0f

        // Show loading dialog when WebView starts
        showLoadingDialog("Loading DegreeWorks...")

        // Custom WebViewClient to detect loading progress
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                dismissLoadingDialog()
                // Fade in the WebView smoothly
                webView.animate().alpha(1f).setDuration(500).start()
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                dismissLoadingDialog()
                showErrorDialog("Failed to load DegreeWorks page. Please check your connection.")
            }
        }

        val url = intent.getStringExtra("url") ?: "https://www.morgan.edu/gateway/"
        webView.loadUrl(url)

        confirmButton.setOnClickListener {
            showLoadingDialog("Processing DegreeWorks data...")

            webView.evaluateJavascript(
                "(function() { return document.documentElement.outerHTML; })();"
            ) { html ->
                Log.i("App", "Captured HTML length: ${html.length}")
                sendHtmlToServer(html)
            }
        }
    }

    private fun sendHtmlToServer(html: String) {
        val client = OkHttpClient()
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val json = """{"html": ${JSONObject.quote(html)}}"""
        val body = json.toRequestBody(mediaType)

        val request = Request.Builder()
            //********************************************
            //********************Change IP address*******
            //********************************************
            .url("http://192.168.1.240:8000/scrape")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("App", "❌ Failed to send HTML to backend", e)
                runOnUiThread {
                    dismissLoadingDialog()
                    showErrorDialog("Failed to send data. Please try again.")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                if (responseData != null) {
                    Log.i("App", "✅ Server response: $responseData")
                    uploadToFirestore(responseData)
                } else {
                    runOnUiThread {
                        dismissLoadingDialog()
                        showErrorDialog("Received empty response from server.")
                    }
                }
            }
        })
    }

    private fun uploadToFirestore(responseData: String) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown_user"

            val topJson = JSONObject(responseData)
            val dataJson = topJson.optJSONObject("data")

            if (dataJson == null) {
                Log.e("App", "⚠️ 'data' field missing or invalid in response")
                runOnUiThread {
                    dismissLoadingDialog()
                    showErrorDialog("Invalid data received from server.")
                }
                return
            }

            val cleanMap = convertToPlainMap(dataJson)

            if (cleanMap is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                firestore.collection("users")
                    .document(userId)
                    .collection("degreeworks")
                    .document("latest")
                    .set(cleanMap as Map<String, Any>)
                    .addOnSuccessListener {
                        Log.i("App", "✅ DegreeWorks data uploaded successfully for $userId")
                        runOnUiThread {
                            dismissLoadingDialog()
                            showSuccessDialog("DegreeWorks data uploaded successfully!")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("App", "❌ Failed to upload DegreeWorks data: ${e.message}")
                        runOnUiThread {
                            dismissLoadingDialog()
                            showErrorDialog("Failed to upload data: ${e.message}")
                        }
                    }
            } else {
                Log.e("App", "⚠️ Converted data is not a valid Map<String, Any>")
                runOnUiThread {
                    dismissLoadingDialog()
                    showErrorDialog("Invalid data format from backend.")
                }
            }
        } catch (e: Exception) {
            Log.e("App", "⚠️ Failed to parse or upload JSON", e)
            runOnUiThread {
                dismissLoadingDialog()
                showErrorDialog("Unexpected error occurred.")
            }
        }
    }

    // Convert any JSON to Firestore-safe Map
    private fun convertToPlainMap(obj: Any?): Any? {
        return when (obj) {
            is JSONObject -> {
                val map = mutableMapOf<String, Any?>()
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    map[key] = convertToPlainMap(obj.get(key))
                }
                map
            }
            is JSONArray -> {
                val list = mutableListOf<Any?>()
                for (i in 0 until obj.length()) {
                    list.add(convertToPlainMap(obj.get(i)))
                }
                list
            }
            JSONObject.NULL -> null
            else -> obj
        }
    }

    private fun showLoadingDialog(message: String) {
        if (loadingDialog?.isShowing == true) return
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null)
        loadingDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        loadingDialog?.show()
    }

    private fun dismissLoadingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    private fun showSuccessDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Success")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .show()
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
