package com.capwords.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap

/** Four white corner brackets that mark the capture target area. */
@Composable
fun ViewfinderFrame(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
) {
    Canvas(modifier = modifier) {
        val len = size.minDimension * 0.18f
        val w = 3f * density
        fun corner(x: Float, y: Float, dx: Float, dy: Float) {
            drawLine(color, Offset(x, y), Offset(x + dx, y), w, StrokeCap.Round)
            drawLine(color, Offset(x, y), Offset(x, y + dy), w, StrokeCap.Round)
        }
        corner(0f, 0f, len, len)                                   // top-left
        corner(size.width, 0f, -len, len)                          // top-right
        corner(0f, size.height, len, -len)                         // bottom-left
        corner(size.width, size.height, -len, -len)                // bottom-right
    }
}
