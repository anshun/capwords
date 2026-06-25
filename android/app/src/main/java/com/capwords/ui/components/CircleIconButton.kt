package com.capwords.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import com.capwords.ui.theme.CapPrimary

/** Round flat icon button used by the capture / recognize toolbars. */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Int = 52,
    emphasized: Boolean = false,
) {
    val bg = if (emphasized) Color.White else Color(0xFFF1F1F0)
    val tint = if (emphasized) CapPrimary else Color(0xFF3A3A3C)
    Surface(
        modifier = modifier.size(size.dp),
        shape = CircleShape,
        color = bg,
        shadowElevation = if (emphasized) 6.dp else 0.dp,
    ) {
        Box(
            modifier = Modifier
                .size(size.dp)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size((size * 0.42f).dp),
            )
        }
    }
}
