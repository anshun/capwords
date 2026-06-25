package com.capwords.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text

/**
 * Bold label with a thick white outline — the "sticker" look the source app gives
 * each recognized word. Rendered as a white stroked layer under a dark fill layer.
 */
@Composable
fun OutlinedLabel(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 22.sp,
    fill: Color = Color(0xFF1C1C1E),
    outline: Color = Color.White,
    strokeWidth: Float = 14f,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontSize = fontSize,
                fontWeight = FontWeight.ExtraBold,
                color = outline,
                drawStyle = Stroke(width = strokeWidth, join = androidx.compose.ui.graphics.StrokeJoin.Round),
            ),
        )
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontSize = fontSize,
                fontWeight = FontWeight.ExtraBold,
                color = fill,
            ),
        )
    }
}

/** A cut-out subject image (transparent background). */
@Composable
fun StickerImage(
    bitmap: ImageBitmap,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    Box(modifier = modifier) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
