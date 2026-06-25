package com.capwords.ui.gallery

import android.graphics.BitmapFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
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
import com.capwords.appContainer
import com.capwords.data.WordEntity
import com.capwords.tts.SpeechHelper
import com.capwords.ui.components.OutlinedLabel
import com.capwords.ui.components.dottedBackground
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

    Box(modifier = Modifier.fillMaxSize().dottedBackground()) {
        if (sections.isEmpty()) {
            Text(
                text = stringResource(R.string.empty_gallery),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(48.dp),
            )
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 64.dp, bottom = 96.dp),
                verticalItemSpacing = 18.dp,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                sections.forEach { section ->
                    item(span = StaggeredGridItemSpan.FullLine, key = "h${section.dayKey}") {
                        Column(modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)) {
                            Text(section.label, style = MaterialTheme.typography.titleLarge)
                            Text(
                                stringResource(R.string.words_count, section.words.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            )
                        }
                    }
                    items(section.words, key = { it.id }) { word ->
                        WordCard(
                            word = word,
                            traditional = traditional,
                            onSpeak = { speech.speakEnglish(word.english) },
                        )
                    }
                }
            }
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 44.dp),
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
) {
    val context = LocalContext.current
    val chinese = if (traditional) word.zhTw else word.zhCn
    val aspect = rememberStickerAspect(word.stickerPath)

    Column(
        modifier = Modifier.fillMaxWidth().noRippleClickable(onSpeak),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(File(word.stickerPath)).build(),
            contentDescription = word.english,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().aspectRatio(aspect),
        )
        OutlinedLabel(text = word.english, modifier = Modifier.padding(top = 2.dp))
        if (!chinese.isNullOrBlank()) {
            Text(
                text = chinese,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

/** Decode just the sticker's bounds (cheap) so the masonry grid sizes by aspect. */
@Composable
private fun rememberStickerAspect(path: String): Float = remember(path) {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, opts)
    if (opts.outWidth > 0 && opts.outHeight > 0) {
        (opts.outWidth.toFloat() / opts.outHeight).coerceIn(0.6f, 1.6f)
    } else 1f
}

@Composable
private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    return clickable(interactionSource = interaction, indication = null, onClick = onClick)
}
