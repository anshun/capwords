package com.capwords.ml

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Phase 1 recognizer backed by ML Kit's bundled image-labeling model
 * (~400 everyday categories, fully on-device / offline).
 *
 * This is a placeholder so the whole flow runs end-to-end today. It is swapped
 * for [ClipRecognizer] in Phase 3 to reach ~20k nouns — no UI changes required.
 */
class MlKitRecognizer(confidenceThreshold: Float = 0.5f) : Recognizer {

    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(confidenceThreshold)
            .build(),
    )

    override suspend fun recognize(bitmap: Bitmap, topK: Int): List<Recognition> =
        suspendCancellableCoroutine { cont ->
            val image = InputImage.fromBitmap(bitmap, 0)
            labeler.process(image)
                .addOnSuccessListener { labels ->
                    val results = labels
                        .sortedByDescending { it.confidence }
                        .take(topK)
                        .map { Recognition(it.text, it.confidence) }
                    cont.resume(results)
                }
                .addOnFailureListener { cont.resumeWithException(it) }
            cont.invokeOnCancellation { /* labeler is reusable; nothing to cancel */ }
        }

    override fun close() {
        labeler.close()
    }
}
