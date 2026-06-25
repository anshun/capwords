package com.capwords.ml

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Phase 1 segmenter using ML Kit Subject Segmentation. Produces a foreground
 * bitmap with a transparent background. If the model isn't available (e.g. fully
 * offline before its first download) it degrades gracefully to the original
 * frame so the flow never breaks.
 */
class MlKitSegmenter : Segmenter {

    private val segmenter = SubjectSegmentation.getClient(
        SubjectSegmenterOptions.Builder()
            .enableForegroundBitmap()
            .build(),
    )

    override suspend fun cutout(bitmap: Bitmap): Bitmap =
        suspendCancellableCoroutine { cont ->
            val image = InputImage.fromBitmap(bitmap, 0)
            segmenter.process(image)
                .addOnSuccessListener { result ->
                    val fg = result.foregroundBitmap
                    cont.resume(fg ?: bitmap)
                }
                .addOnFailureListener { e ->
                    Log.w("MlKitSegmenter", "segmentation unavailable, using full frame", e)
                    cont.resume(bitmap)
                }
        }

    override fun close() {
        segmenter.close()
    }
}
