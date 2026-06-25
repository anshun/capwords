package com.capwords.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.sqrt

/**
 * Phase 3 recognizer: open-vocabulary recognition via a MobileCLIP image encoder
 * matched against a pre-computed table of ~20k English-noun text embeddings.
 *
 * Required assets (produced by tools/ in Phase 2-3):
 *   - assets/mobileclip_image.tflite   image encoder, input [1,224,224,3] f32, output [1,D]
 *   - assets/clip_words.txt            N English nouns, one per line
 *   - assets/text_embeddings.bin       N*D little-endian float32, L2-normalized, row-aligned to clip_words.txt
 *
 * On device we encode the frame once and take the nearest neighbour in the text table.
 * Adding more words = ship a bigger table; no code change.
 */
class ClipRecognizer private constructor(
    private val interpreter: Interpreter,
    private val words: List<String>,
    private val textMatrix: FloatArray, // N * dim, row-major, L2-normalized
    private val dim: Int,
) : Recognizer {

    private val inputSize = 224
    private val inputBuffer: ByteBuffer =
        ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4).order(ByteOrder.nativeOrder())
    private val outputBuffer = Array(1) { FloatArray(dim) }

    override suspend fun recognize(bitmap: Bitmap, topK: Int): List<Recognition> =
        withContext(Dispatchers.Default) {
            val embedding = encodeImage(bitmap)
            val n = words.size
            // Cosine similarity == dot product (both sides L2-normalized).
            val scored = ArrayList<Recognition>(n)
            var row = 0
            for (i in 0 until n) {
                var dot = 0f
                val base = row
                for (d in 0 until dim) dot += embedding[d] * textMatrix[base + d]
                scored.add(Recognition(words[i], dot))
                row += dim
            }
            scored.sortByDescending { it.confidence }
            scored.take(topK)
        }

    private fun encodeImage(bitmap: Bitmap): FloatArray {
        val scaled = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        inputBuffer.rewind()
        val pixels = IntArray(inputSize * inputSize)
        scaled.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        // CLIP normalization (ImageNet-ish mean/std on 0..1 range).
        val mean = floatArrayOf(0.48145466f, 0.4578275f, 0.40821073f)
        val std = floatArrayOf(0.26862954f, 0.26130258f, 0.27577711f)
        for (p in pixels) {
            val r = ((p shr 16) and 0xFF) / 255f
            val g = ((p shr 8) and 0xFF) / 255f
            val b = (p and 0xFF) / 255f
            inputBuffer.putFloat((r - mean[0]) / std[0])
            inputBuffer.putFloat((g - mean[1]) / std[1])
            inputBuffer.putFloat((b - mean[2]) / std[2])
        }
        interpreter.run(inputBuffer, outputBuffer)
        return l2normalize(outputBuffer[0])
    }

    override fun close() {
        interpreter.close()
    }

    companion object {
        private const val TAG = "ClipRecognizer"
        private const val MODEL = "mobileclip_image.tflite"
        private const val WORDS = "clip_words.txt"
        private const val EMB = "text_embeddings.bin"

        /** True only when every required asset is bundled. */
        fun assetsAvailable(context: Context): Boolean {
            val names = runCatching { context.assets.list("")?.toSet() ?: emptySet() }
                .getOrDefault(emptySet())
            return MODEL in names && WORDS in names && EMB in names
        }

        suspend fun create(context: Context): ClipRecognizer = withContext(Dispatchers.IO) {
            val model = loadModelFile(context, MODEL)
            val options = Interpreter.Options().apply { setNumThreads(4) }
            val interpreter = Interpreter(model, options)
            val words = context.assets.open(WORDS).bufferedReader().useLines { it.toList() }
            val emb = context.assets.open(EMB).use { it.readBytes() }
            val floats = ByteBuffer.wrap(emb).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
            val total = floats.remaining()
            val matrix = FloatArray(total).also { floats.get(it) }
            val dim = total / words.size
            Log.i(TAG, "Loaded ${words.size} words, dim=$dim")
            ClipRecognizer(interpreter, words, matrix, dim)
        }

        private fun loadModelFile(context: Context, name: String): ByteBuffer {
            val fd = context.assets.openFd(name)
            FileInputStream(fd.fileDescriptor).use { input ->
                return input.channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    fd.startOffset,
                    fd.declaredLength,
                )
            }
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
