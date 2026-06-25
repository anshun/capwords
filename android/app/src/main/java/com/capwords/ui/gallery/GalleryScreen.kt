package com.capwords.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.capwords.R
import com.capwords.data.WordEntity
import com.capwords.tts.SpeechHelper
import com.capwords.appContainer
import com.capwords.ui.components.OutlinedLabel
import com.capwords.ui.theme.CapBackground
import java.io.File

@Composable
fun GalleryScreen(
    speech: SpeechHelper,
    onBack: () -> Unit,
    onAddNew: () -> Unit,
    viewModel: GalleryViewModel = viewModel(),
) {
    val sections by viewModel.sections.collectAsState()
    val context = LocalContext.current
    val traditional = context.appContainer.settings.traditionalChinese

    Box(modifier = Modifier.fillMaxSize().background(CapBackground)) {
        if (sections.isEmpty()) {
            Text(
                text = stringResource(R.string.empty_gallery),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(48.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, top = 64.dp, bottom = 96.dp,
                ),
            ) {
                sections.forEach { section ->
                    item(key = "header_${section.dayKey}") {
                        Column(modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 12.dp)) {
                            Text(section.label, style = MaterialTheme.typography.titleLarge)
                            Text(
                                stringResource(R.string.words_count, section.words.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            )
                        }
                    }
                    items(section.words.chunked(2), key = { it.first().id }) { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            rowItems.forEach { word ->
                                WordCard(
                                    word = word,
                                    traditional = traditional,
                                    onSpeak = { speech.speakEnglish(word.english) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Top bar back button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp, top = 44.dp),
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
        }
    }
}

@Composable
private fun WordCard(
    word: WordEntity,
    traditional: Boolean,
    onSpeak: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val chinese = if (traditional) word.zhTw else word.zhCn
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(File(word.stickerPath)).build(),
                contentDescription = word.english,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(8.dp),
            )
        }
        OutlinedLabel(
            text = word.english,
            modifier = Modifier
                .padding(top = 2.dp)
                .noRippleClickable(onSpeak),
        )
        if (!chinese.isNullOrBlank()) {
            Text(
                text = chinese,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    return clickable(interactionSource = interaction, indication = null, onClick = onClick)
}
