package com.capwords.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Subtle dot-grid texture used on the light screens, like the source app's
 * gallery/result backgrounds. Drawn behind content; cheap (one drawBehind pass).
 */
fun Modifier.dottedBackground(
    base: Color = Color(0xFFF6F6F4),
    dot: Color = Color(0x14000000),
    spacing: Dp = 22.dp,
    radius: Dp = 1.2.dp,
): Modifier = this
    .background(base)
    .drawBehind {
        val step = spacing.toPx()
        val r = radius.toPx()
        var y = step / 2
        while (y < size.height) {
            var x = step / 2
            while (x < size.width) {
                drawCircle(color = dot, radius = r, center = Offset(x, y))
                x += step
            }
            y += step
        }
    }
