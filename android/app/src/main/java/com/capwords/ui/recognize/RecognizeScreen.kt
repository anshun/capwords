package com.capwords.ui.recognize

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.capwords.R
import com.capwords.appContainer
import com.capwords.tts.SpeechHelper
import com.capwords.ui.components.CircleIconButton
import com.capwords.ui.components.OutlinedLabel
import com.capwords.ui.components.StickerImage
import com.capwords.ui.components.dottedBackground
import com.capwords.ui.flow.CaptureFlowViewModel

@Composable
fun RecognizeScreen(
    flowViewModel: CaptureFlowViewModel,
    speech: SpeechHelper,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
) {
    val state by flowViewModel.state.collectAsState()
    val context = LocalContext.current
    val traditional = remember { context.appContainer.settings.traditionalChinese }
    var showAdjust by remember { mutableStateOf(false) }

    val sticker = state.sticker ?: state.captured
    val chinese = if (traditional) state.translation?.zhTw else state.translation?.zhCn

    val resultReady = !state.processing && state.selected != null
    val stickerScale by animateFloatAsState(
        targetValue = if (resultReady) 1f else 0.82f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "stickerScale",
    )

    Box(modifier = Modifier.fillMaxSize().dottedBackground()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Soft yellow glow behind the sticker (source-app result look).
                if (resultReady) {
                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0x66FFE9A8), Color(0x00FFE9A8)),
                                ),
                            ),
                    )
                }
                if (sticker != null) {
                    StickerImage(
                        bitmap = sticker.asImageBitmap(),
                        modifier = Modifier.size(260.dp).scale(stickerScale),
                    )
                }
                if (state.processing) {
                    CircularProgressIndicator(color = Color(0xFFB0B0B0))
                }
            }

            Spacer(Modifier.height(20.dp))

            if (!state.processing) {
                val word = state.selected?.english
                if (word != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedLabel(text = word, fontSize = 28.sp)
                        IconButton(onClick = { speech.speakEnglish(word) }) {
                            Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = stringResource(R.string.speak))
                        }
                    }
                    if (chinese != null) {
                        Text(
                            text = chinese,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF3A3A3C),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.recognizing),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                // Adjust affordance + Top-5 chips
                Text(
                    text = stringResource(R.string.not_expected),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF8E8E93),
                    modifier = Modifier
                        .padding(top = 18.dp)
                        .noRippleClickable { showAdjust = !showAdjust },
                )
                AnimatedVisibility(visible = showAdjust) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.candidates.forEach { rec ->
                            AssistChip(
                                onClick = { flowViewModel.selectCandidate(rec) },
                                label = { Text(rec.english) },
                            )
                        }
                    }
                }
            }
        }

        // Bottom toolbar: retake / save / cancel
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .navigationBarsPadding()
                .padding(vertical = 22.dp, horizontal = 36.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(
                icon = Icons.Outlined.Refresh,
                contentDescription = stringResource(R.string.retake),
                onClick = { flowViewModel.reset(); onCancel() },
                size = 52,
            )
            CircleIconButton(
                icon = Icons.Outlined.Check,
                contentDescription = stringResource(R.string.confirm),
                onClick = {
                    flowViewModel.save(System.currentTimeMillis()) { onSaved() }
                },
                size = 64,
                emphasized = true,
            )
            CircleIconButton(
                icon = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.cancel),
                onClick = { flowViewModel.reset(); onCancel() },
                size = 52,
            )
        }
    }
}

@Composable
private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    return clickable(interactionSource = interaction, indication = null, onClick = onClick)
}
