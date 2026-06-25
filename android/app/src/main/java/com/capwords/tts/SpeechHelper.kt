package com.capwords.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Thin wrapper over Android's offline TextToSpeech engine. Supports English and
 * both Chinese variants; the engine uses on-device voice data once installed.
 */
class SpeechHelper(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech = TextToSpeech(context.applicationContext, this)
    private var ready = false

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (!ready) Log.w(TAG, "TextToSpeech init failed: $status")
    }

    fun speakEnglish(text: String) = speak(text, Locale.US, "en")

    fun speakChinese(text: String, traditional: Boolean) {
        val locale = if (traditional) Locale.TRADITIONAL_CHINESE else Locale.SIMPLIFIED_CHINESE
        speak(text, locale, if (traditional) "zhTW" else "zhCN")
    }

    private fun speak(text: String, locale: Locale, utteranceId: String) {
        if (!ready || text.isBlank()) return
        val result = tts.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Language not available: $locale")
            return
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }

    companion object {
        private const val TAG = "SpeechHelper"
    }
}
