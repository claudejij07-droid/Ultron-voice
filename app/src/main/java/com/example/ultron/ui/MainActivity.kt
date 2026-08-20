package com.example.ultron.ui

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.ultron.BuildConfig
import com.example.ultron.R
import com.example.ultron.service.UltronService
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private val client = OkHttpClient()
    private val apiKey = BuildConfig.ANTHROPIC_API_KEY
    private lateinit var statusText: TextView
    private lateinit var inputText: EditText
    private lateinit var sendButton: Button
    private lateinit var voiceButton: Button
    private val VOICE_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        inputText = findViewById(R.id.inputText)
        sendButton = findViewById(R.id.sendButton)
        voiceButton = findViewById(R.id.voiceButton)

        if (UltronService.isAccessibilityEnabled(this)) {
            statusText.text = "Ultron Active ✓"
        } else {
            statusText.text = "Ultron Inactive"
            UltronService.openAccessibilitySettings(this)
        }

        sendButton.setOnClickListener {
            val command = inputText.text.toString()
            if (command.isNotEmpty()) {
                statusText.text = "Thinking..."
                sendToClaude(command)
            }
        }

        voiceButton.setOnClickListener {
            startVoiceRecognition()
        }
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your command")
        }
        try {
            @Suppress("DEPRECATION")
            startActivityForResult(intent, VOICE_REQUEST_CODE)
        } catch (e: Exception) {
            statusText.text = "Voice recognition not available"
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VOICE_REQUEST_CODE && resultCode == RESULT_OK) {
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.get(0) ?: ""
            if (spokenText.isNotEmpty()) {
                inputText.setText(spokenText)
                statusText.text = "Thinking..."
                sendToClaude(spokenText)
            }
        }
    }

    private fun sendToClaude(command: String) {
        val json = JSONObject().apply {
            put("model", "claude-sonnet-4-6")
            put("max_tokens", 1024)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", command)
                })
            })
        }

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { statusText.text = "Error: ${e.message}" }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                val reply = JSONObject(body ?: "{}")
                    .optJSONArray("content")
                    ?.optJSONObject(0)
                    ?.optString("text") ?: "No response"
                runOnUiThread {
                    statusText.text = reply
                    inputText.text.clear()
                }
            }
        })
    }
}
