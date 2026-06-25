package com.capwords.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.capwords.ui.theme.RainbowSweep

/**
 * The colorful capture button from the source app: a slowly rotating rainbow ring
 * around a white core.
 */
@Composable
fun RainbowShutter(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Int = 76,
) {
    val transition = rememberInfiniteTransition(label = "shutter")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "angle",
    )
    val interaction = remember { MutableInteractionSource() }

    Canvas(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = false),
                onClick = onClick,
            )
            .rotate(angle),
    ) {
        val stroke = this.size.minDimension * 0.16f
        val radius = (this.size.minDimension - stroke) / 2f
        drawCircle(color = Color.White, radius = this.size.minDimension / 2f)
        drawCircle(
            brush = Brush.sweepGradient(RainbowSweep),
            radius = radius,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
        )
    }
}
