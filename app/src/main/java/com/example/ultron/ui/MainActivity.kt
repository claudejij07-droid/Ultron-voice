package com.example.ultron.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.webkit.WebView
import android.widget.EditText
import android.widget.ImageButton
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
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private val client = OkHttpClient()
    private val apiKey = BuildConfig.GROQ_API_KEY
    private lateinit var statusText: TextView
    private lateinit var greetingText: TextView
    private lateinit var inputText: EditText
    private lateinit var voiceButton: ImageButton
    private lateinit var orbWebView: WebView
    private lateinit var tts: TextToSpeech
    private val VOICE_REQUEST_CODE = 100

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tts = TextToSpeech(this, this)

        statusText = findViewById(R.id.statusText)
        greetingText = findViewById(R.id.greetingText)
        inputText = findViewById(R.id.inputText)
        voiceButton = findViewById(R.id.voiceButton)
        orbWebView = findViewById(R.id.orbWebView)

        orbWebView.settings.javaScriptEnabled = true
        orbWebView.settings.domStorageEnabled = true
        orbWebView.setBackgroundColor(0)
        orbWebView.loadUrl("file:///android_asset/arc_reactor.html")

        if (UltronService.isAccessibilityEnabled(this)) {
            greetingText.text = "Ultron is listening..."
        } else {
            greetingText.text = "Ultron needs accessibility access"
            UltronService.openAccessibilitySettings(this)
        }

        inputText.setOnEditorActionListener { _, _, _ ->
            val command = inputText.text.toString()
            if (command.isNotEmpty()) {
                statusText.text = "Thinking..."
                sendToGroq(command)
            }
            true
        }

        voiceButton.setOnClickListener {
            startVoiceRecognition()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.UK)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.language = Locale.US
            }
            tts.setPitch(0.7f)
            tts.speak("Ultron online, how can I help you, sir", TextToSpeech.QUEUE_FLUSH, null, null)
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
                sendToGroq(spokenText)
            }
        }
    }

    private fun sendToGroq(command: String) {
        if (apiKey.isEmpty()) {
            statusText.text = "⚠️ No API key found in build!"
            return
        }

        val json = JSONObject().apply {
            put("model", "openai/gpt-oss-120b")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", command)
                })
            })
        }

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("content-type", "application/json")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { statusText.text = "Network error: ${e.message}" }
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyStr = response.body?.string() ?: "{}"
                if (!response.isSuccessful) {
                    runOnUiThread { statusText.text = "API error ${response.code}: $bodyStr" }
                    return
                }
                val reply = JSONObject(bodyStr)
                    .optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content") ?: "Empty response"
                runOnUiThread {
                    statusText.text = reply
                    inputText.text.clear()
                    tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }
        })
    }
}
