package com.capwords.ml

import android.content.Context
import android.graphics.Bitmap
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer

/**
 * Fully-offline subject cut-out using U²-Net (u2netp, ~4.4MB) via ONNX Runtime.
 *
 * Asset: assets/u2netp.onnx — input "input.1" [1,3,320,320] f32 NCHW, 7 sigmoid
 * outputs [1,1,320,320]; output 0 is the saliency map. Preprocessing matches the
 * U²-Net reference: divide by per-image max, then ImageNet mean/std, NCHW.
 *
 * Produces a sticker: original RGB with the saliency map as the alpha channel
 * (transparent background), like the source app's cut-outs.
 */
class U2NetSegmenter private constructor(
    private val env: OrtEnvironment,
    private val session: OrtSession,
) : Segmenter {

    private val size = 320
    private val inputName: String = session.inputNames.first()
    private val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
    private val std = floatArrayOf(0.229f, 0.224f, 0.225f)

    override suspend fun cutout(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val mask = runMask(bitmap)          // size*size, [0,1], min-max normalized
        applyAlpha(bitmap, mask)
    }

    private fun runMask(bitmap: Bitmap): FloatArray {
        val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val pixels = IntArray(size * size)
        scaled.getPixels(pixels, 0, size, 0, 0, size, size)

        var maxv = 1
        for (p in pixels) {
            maxv = maxOf(maxv, (p shr 16) and 0xFF, (p shr 8) and 0xFF, p and 0xFF)
        }
        val maxf = maxv.toFloat()
        val area = size * size
        val chw = FloatArray(3 * area)
        for (i in 0 until area) {
            val p = pixels[i]
            chw[i] = (((p shr 16) and 0xFF) / maxf - mean[0]) / std[0]
            chw[area + i] = (((p shr 8) and 0xFF) / maxf - mean[1]) / std[1]
            chw[2 * area + i] = ((p and 0xFF) / maxf - mean[2]) / std[2]
        }

        val shape = longArrayOf(1, 3, size.toLong(), size.toLong())
        OnnxTensor.createTensor(env, FloatBuffer.wrap(chw), shape).use { tensor ->
            session.run(mapOf(inputName to tensor)).use { result ->
                val out = (result[0] as OnnxTensor).floatBuffer
                val m = FloatArray(area)
                out.get(m)
                var lo = Float.MAX_VALUE
                var hi = -Float.MAX_VALUE
                for (v in m) { if (v < lo) lo = v; if (v > hi) hi = v }
                val range = (hi - lo).coerceAtLeast(1e-6f)
                for (i in m.indices) m[i] = (m[i] - lo) / range
                return m
            }
        }
    }

    /** Original RGB with the (upscaled) saliency map as alpha. */
    private fun applyAlpha(src: Bitmap, mask: FloatArray): Bitmap {
        val w = src.width
        val h = src.height
        val srcPix = IntArray(w * h)
        src.getPixels(srcPix, 0, w, 0, 0, w, h)
        val outPix = IntArray(w * h)
        for (y in 0 until h) {
            val my = (y * size) / h
            for (x in 0 until w) {
                val mx = (x * size) / w
                val a = (mask[my * size + mx] * 255f).toInt().coerceIn(0, 255)
                outPix[y * w + x] = (a shl 24) or (srcPix[y * w + x] and 0x00FFFFFF)
            }
        }
        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            setPixels(outPix, 0, w, 0, 0, w, h)
        }
    }

    override fun close() {
        session.close()
    }

    companion object {
        private const val MODEL = "u2netp.onnx"

        fun assetAvailable(context: Context): Boolean =
            runCatching { context.assets.list("")?.contains(MODEL) ?: false }.getOrDefault(false)

        suspend fun create(context: Context): U2NetSegmenter = withContext(Dispatchers.IO) {
            val env = OrtEnvironment.getEnvironment()
            val opts = OrtSession.SessionOptions().apply { setIntraOpNumThreads(4) }
            val bytes = context.assets.open(MODEL).use { it.readBytes() }
            U2NetSegmenter(env, env.createSession(bytes, opts))
        }
    }
}
