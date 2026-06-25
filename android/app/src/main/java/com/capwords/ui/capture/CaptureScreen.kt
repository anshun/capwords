package com.capwords.ui.capture

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.capwords.ui.components.CircleIconButton
import com.capwords.ui.components.dottedBackground
import com.capwords.ui.flow.CaptureFlowViewModel

@Composable
fun CaptureScreen(
    flowViewModel: CaptureFlowViewModel,
    onConfirmed: () -> Unit,
    onCancel: () -> Unit,
) {
    val state by flowViewModel.state.collectAsState()
    val captured = state.captured

    Box(modifier = Modifier.fillMaxSize().dottedBackground()) {
        if (captured != null) {
            Image(
                bitmap = captured.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(24.dp),
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.White, androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .navigationBarsPadding()
                .padding(vertical = 22.dp, horizontal = 36.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(
                icon = Icons.Outlined.Crop,
                contentDescription = "Crop",
                onClick = { /* Phase 4: manual crop */ },
                size = 52,
            )
            CircleIconButton(
                icon = Icons.Outlined.Check,
                contentDescription = "Confirm",
                onClick = {
                    flowViewModel.confirm()
                    onConfirmed()
                },
                size = 64,
                emphasized = true,
            )
            CircleIconButton(
                icon = Icons.Outlined.Close,
                contentDescription = "Cancel",
                onClick = {
                    flowViewModel.reset()
                    onCancel()
                },
                size = 52,
            )
        }
    }
}
