package com.capwords.ui.flow

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.capwords.appContainer
import com.capwords.ml.Recognition
import com.capwords.ml.Translation
import com.capwords.ui.util.BitmapUtils.trimToAlpha
import com.capwords.ui.util.BitmapUtils.withWhiteStickerBorder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** UI state shared across the Camera -> Capture -> Recognize screens. */
data class FlowState(
    val captured: Bitmap? = null,
    val sticker: Bitmap? = null,
    val candidates: List<Recognition> = emptyList(),
    val selected: Recognition? = null,
    val translation: Translation? = null,
    val processing: Boolean = false,
    val saved: Boolean = false,
)

class CaptureFlowViewModel(app: Application) : AndroidViewModel(app) {

    private val container = app.appContainer

    private val _state = MutableStateFlow(FlowState())
    val state: StateFlow<FlowState> = _state.asStateFlow()

    /** Live top label shown over the camera viewfinder. */
    private val _liveLabel = MutableStateFlow<String?>(null)
    val liveLabel: StateFlow<String?> = _liveLabel.asStateFlow()

    @Volatile
    private var analyzing = false

    /** Called from the throttled camera analyzer; best-effort, drops frames while busy. */
    fun analyzeFrame(frame: Bitmap) {
        if (analyzing) return
        analyzing = true
        viewModelScope.launch {
            try {
                val top = container.recognizer().recognize(frame, topK = 1).firstOrNull()
                _liveLabel.value = top?.english
            } catch (_: Throwable) {
                // ignore transient analyzer failures
            } finally {
                analyzing = false
            }
        }
    }

    /** Shutter pressed: keep the full frame and move to the capture/confirm screen. */
    fun onCaptured(bitmap: Bitmap) {
        _state.value = FlowState(captured = bitmap)
    }

    /** Confirm: segment the subject, recognize it, translate the top candidate. */
    fun confirm() {
        val captured = _state.value.captured ?: return
        _state.value = _state.value.copy(processing = true)
        viewModelScope.launch {
            container.translator.ensureLoaded()
            val cutout = runCatching { container.segmenter().cutout(captured) }
                .getOrDefault(captured)
            // Recognize the segmented subject (raw cut-out), not the whole scene:
            // isolating the object dramatically improves accuracy (a donut on a big
            // plate would otherwise be read as "plate").
            val candidates = runCatching {
                container.recognizer().recognize(cutout, topK = 5)
            }.getOrDefault(emptyList())
            val top = candidates.firstOrNull()
            // Display/save a trimmed, white "die-cut" sticker (source-app look).
            val sticker = runCatching { cutout.trimToAlpha().withWhiteStickerBorder() }
                .getOrDefault(cutout)
            _state.value = _state.value.copy(
                sticker = sticker,
                candidates = candidates,
                selected = top,
                translation = top?.let { container.translator.translate(it.english) },
                processing = false,
            )
        }
    }

    /** User picks a different candidate from the Top-5 list. */
    fun selectCandidate(rec: Recognition) {
        _state.value = _state.value.copy(
            selected = rec,
            translation = container.translator.translate(rec.english),
        )
    }

    /** Persist the current selection + sticker into the gallery. */
    fun save(createdAt: Long, onDone: () -> Unit) {
        val s = _state.value
        val selected = s.selected ?: return
        val sticker = s.sticker ?: s.captured ?: return
        viewModelScope.launch {
            container.repository.saveWord(
                english = selected.english,
                zhTw = s.translation?.zhTw,
                zhCn = s.translation?.zhCn,
                confidence = selected.confidence,
                sticker = sticker,
                createdAt = createdAt,
            )
            _state.value = _state.value.copy(saved = true)
            onDone()
        }
    }

    fun reset() {
        _state.value = FlowState()
    }
}
