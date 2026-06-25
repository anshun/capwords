package com.capwords

import android.content.Context
import java.util.Locale

/** Lightweight persisted user preferences (Chinese variant for display + TTS). */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("capwords_prefs", Context.MODE_PRIVATE)

    /** True = Traditional (zh-TW), false = Simplified (zh-CN). Defaults from locale. */
    var traditionalChinese: Boolean
        get() = prefs.getBoolean(KEY_TRADITIONAL, defaultTraditional())
        set(value) = prefs.edit().putBoolean(KEY_TRADITIONAL, value).apply()

    private fun defaultTraditional(): Boolean {
        val l = Locale.getDefault()
        // TW / HK / MO lean Traditional; everything else defaults to Simplified.
        return l.country in setOf("TW", "HK", "MO") ||
            l.script == "Hant"
    }

    companion object {
        private const val KEY_TRADITIONAL = "traditional_chinese"
    }
}
