package com.capwords.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.sqrt

/**
 * Open-vocabulary recognition via a MobileCLIP-S0 image encoder (ONNX Runtime)
 * matched against a pre-computed table of ~20k English-noun text embeddings.
 *
 * Required assets (produced by tools/ in Phase 3):
 *   - assets/mobileclip_image.onnx   image encoder, input "image" [1,3,256,256] f32 (NCHW, 0..1), output [1,512] (L2-normalized)
 *   - assets/clip_words.txt          N English nouns, one per line
 *   - assets/text_embeddings.bin     N*512 little-endian float32, L2-normalized, row-aligned to clip_words.txt
 *
 * On device we encode the frame once and take the nearest neighbour in the text
 * table. Adding more words = ship a bigger table; no code change.
 */
class ClipRecognizer private constructor(
    private val env: OrtEnvironment,
    private val session: OrtSession,
    private val words: List<String>,
    private val textMatrix: FloatArray, // N * dim, row-major, L2-normalized
    private val dim: Int,
) : Recognizer {

    private val inputSize = 256
    private val inputName: String = session.inputNames.first()

    override suspend fun recognize(bitmap: Bitmap, topK: Int): List<Recognition> =
        withContext(Dispatchers.Default) {
            val embedding = encodeImage(bitmap)
            val n = words.size
            val scored = ArrayList<Recognition>(n)
            var row = 0
            for (i in 0 until n) {
                var dot = 0f
                for (d in 0 until dim) dot += embedding[d] * textMatrix[row + d]
                scored.add(Recognition(words[i], dot))
                row += dim
            }
            scored.sortByDescending { it.confidence }
            scored.take(topK)
        }

    /** MobileCLIP-S0: 256x256, NCHW planar, values 0..1, no mean/std. */
    private fun encodeImage(bitmap: Bitmap): FloatArray {
        val scaled = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val pixels = IntArray(inputSize * inputSize)
        scaled.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        val area = inputSize * inputSize
        val chw = FloatArray(3 * area)
        for (i in 0 until area) {
            val p = pixels[i]
            // Alpha-composite over white so a cut-out sticker's transparent
            // background doesn't leak the original scene into recognition.
            val a = (p ushr 24) and 0xFF
            val r = (((p shr 16) and 0xFF) * a + 255 * (255 - a)) / 255
            val g = (((p shr 8) and 0xFF) * a + 255 * (255 - a)) / 255
            val b = ((p and 0xFF) * a + 255 * (255 - a)) / 255
            chw[i] = r / 255f                 // R plane
            chw[area + i] = g / 255f           // G plane
            chw[2 * area + i] = b / 255f       // B plane
        }

        val buffer = FloatBuffer.wrap(chw)
        val shape = longArrayOf(1, 3, inputSize.toLong(), inputSize.toLong())
        OnnxTensor.createTensor(env, buffer, shape).use { tensor ->
            session.run(mapOf(inputName to tensor)).use { result ->
                @Suppress("UNCHECKED_CAST")
                val out = (result[0].value as Array<FloatArray>)[0]
                return l2normalize(out)
            }
        }
    }

    override fun close() {
        session.close()
    }

    companion object {
        private const val TAG = "ClipRecognizer"
        private const val MODEL = "mobileclip_image.onnx"
        private const val WORDS = "clip_words.txt"
        private const val EMB = "text_embeddings.bin"

        /** True only when every required asset is bundled. */
        fun assetsAvailable(context: Context): Boolean {
            val names = runCatching { context.assets.list("")?.toSet() ?: emptySet() }
                .getOrDefault(emptySet())
            return MODEL in names && WORDS in names && EMB in names
        }

        suspend fun create(context: Context): ClipRecognizer = withContext(Dispatchers.IO) {
            val env = OrtEnvironment.getEnvironment()
            val opts = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(4)
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            }
            val modelBytes = context.assets.open(MODEL).use { it.readBytes() }
            val session = env.createSession(modelBytes, opts)

            val words = context.assets.open(WORDS).bufferedReader().useLines { it.toList() }
            // int8 embedding table: float32 scale (LE) + N*dim int8, dequant = q * scale.
            val emb = context.assets.open(EMB).use { it.readBytes() }
            val bb = ByteBuffer.wrap(emb).order(ByteOrder.LITTLE_ENDIAN)
            val scale = bb.float
            val count = emb.size - 4
            val matrix = FloatArray(count)
            for (i in 0 until count) matrix[i] = bb.get().toInt() * scale
            val dim = count / words.size
            Log.i(TAG, "Loaded ${words.size} words, dim=$dim, scale=$scale")
            ClipRecognizer(env, session, words, matrix, dim)
        }

        private fun l2normalize(v: FloatArray): FloatArray {
            var sum = 0f
            for (x in v) sum += x * x
            val norm = sqrt(sum).coerceAtLeast(1e-8f)
            for (i in v.indices) v[i] /= norm
            return v
        }
    }
}
