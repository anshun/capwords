package com.capwords.ml

import android.graphics.Bitmap

/** A single candidate label for an image. */
data class Recognition(
    val english: String,
    val confidence: Float,
)

/**
 * Recognizes the dominant object in a still image and returns the top-k English
 * labels, highest confidence first.
 *
 * Phase 1 implementation: [MlKitRecognizer] (bundled ~400-class on-device model).
 * Phase 3 implementation: [ClipRecognizer] (MobileCLIP open-vocabulary, ~20k nouns).
 * Both honour this interface so the UI never changes.
 */
interface Recognizer {
    suspend fun recognize(bitmap: Bitmap, topK: Int = 5): List<Recognition>
    fun close() {}
}
