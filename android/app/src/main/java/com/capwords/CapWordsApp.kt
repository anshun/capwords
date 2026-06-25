package com.capwords

import android.app.Application
import android.content.Context
import com.capwords.data.AppDatabase
import com.capwords.data.WordRepository
import com.capwords.ml.ClipRecognizer
import com.capwords.ml.MlKitRecognizer
import com.capwords.ml.MlKitSegmenter
import com.capwords.ml.Recognizer
import com.capwords.ml.Segmenter
import com.capwords.ml.U2NetSegmenter
import com.capwords.ml.Translator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CapWordsApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/**
 * Hand-rolled DI container. Keeps Phase 1 dependency-light; swap implementations
 * here without touching the UI.
 */
class AppContainer(private val context: Context) {

    val settings: Settings by lazy { Settings(context) }

    val repository: WordRepository by lazy {
        WordRepository(context, AppDatabase.get(context).wordDao())
    }

    val translator: Translator by lazy { Translator(context) }

    private val segmenterMutex = Mutex()
    private var cachedSegmenter: Segmenter? = null

    /**
     * Returns the best available segmenter: bundled U²-Net (fully offline) when
     * its asset is present, otherwise ML Kit subject segmentation.
     */
    suspend fun segmenter(): Segmenter = segmenterMutex.withLock {
        cachedSegmenter ?: run {
            val s = if (U2NetSegmenter.assetAvailable(context)) {
                U2NetSegmenter.create(context)
            } else {
                MlKitSegmenter()
            }
            cachedSegmenter = s
            s
        }
    }

    private val recognizerMutex = Mutex()
    private var cachedRecognizer: Recognizer? = null

    /**
     * Returns the best available recognizer: MobileCLIP (~20k nouns) once its
     * assets are bundled (Phase 3), otherwise the ML Kit fallback (~400 classes).
     */
    suspend fun recognizer(): Recognizer = recognizerMutex.withLock {
        cachedRecognizer ?: run {
            val r = if (ClipRecognizer.assetsAvailable(context)) {
                ClipRecognizer.create(context)
            } else {
                MlKitRecognizer()
            }
            cachedRecognizer = r
            r
        }
    }
}

/** Convenience accessor from any Context. */
val Context.appContainer: AppContainer
    get() = (applicationContext as CapWordsApp).container
