package com.capwords.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.capwords.tts.SpeechHelper

/** Provides a [SpeechHelper] tied to the composition lifecycle. */
@Composable
fun rememberSpeechHelper(): SpeechHelper {
    val context = LocalContext.current
    val helper = remember { SpeechHelper(context) }
    DisposableEffect(Unit) {
        onDispose { helper.shutdown() }
    }
    return helper
}
