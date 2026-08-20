package com.example.ultron.service

import android.content.Intent
import android.service.voice.VoiceInteractionService

class UltronVoiceSession : VoiceInteractionService() {
    override fun onReady() {
        super.onReady()
        val overlayIntent = Intent(this, OverlayService::class.java)
        startService(overlayIntent)
    }
}
