package com.capwords.ml

import android.graphics.Bitmap

/**
 * Cuts the main subject out of a photo, returning a sticker bitmap with a
 * transparent background (and, in [StickerStyle], a white outline like the
 * source app).
 *
 * Phase 1: [MlKitSegmenter] (on-device subject segmentation).
 * Phase 3: U2NetSegmenter (bundled u2netp.tflite, fully offline-on-install).
 */
interface Segmenter {
    suspend fun cutout(bitmap: Bitmap): Bitmap
    fun close() {}
}
