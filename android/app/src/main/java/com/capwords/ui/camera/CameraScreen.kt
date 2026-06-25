package com.capwords.ui.camera

import android.os.SystemClock
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.capwords.R
import com.capwords.ui.components.RainbowShutter
import com.capwords.ui.components.ViewfinderFrame
import com.capwords.ui.flow.CaptureFlowViewModel
import com.capwords.ui.util.BitmapUtils.scaledForAnalysis
import com.capwords.ui.util.BitmapUtils.toUprightBitmap
import com.capwords.ui.util.DateUtils
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    flowViewModel: CaptureFlowViewModel,
    onCaptured: () -> Unit,
    onOpenGallery: () -> Unit,
) {
    CameraPermissionGate {
        CameraContent(
            flowViewModel = flowViewModel,
            onCaptured = onCaptured,
            onOpenGallery = onOpenGallery,
        )
    }
}

@Composable
private fun CameraContent(
    flowViewModel: CaptureFlowViewModel,
    onCaptured: () -> Unit,
    onOpenGallery: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val liveLabel by flowViewModel.liveLabel.collectAsState()

    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    androidx.compose.runtime.DisposableEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        var lastAnalysis = 0L
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { ia ->
                    ia.setAnalyzer(analysisExecutor) { proxy: ImageProxy ->
                        val now = SystemClock.uptimeMillis()
                        if (now - lastAnalysis >= ANALYSIS_INTERVAL_MS) {
                            lastAnalysis = now
                            runCatching {
                                flowViewModel.analyzeFrame(proxy.toUprightBitmap().scaledForAnalysis())
                            }
                        }
                        proxy.close()
                    }
                }
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture,
                analysis,
            )
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            analysisExecutor.shutdown()
            ProcessCameraProvider.getInstance(context).get().unbindAll()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // Top: date + hint
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 20.dp, top = 12.dp),
        ) {
            Text(
                text = DateUtils.dayLabel(System.currentTimeMillis()),
                color = Color.White,
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = liveLabel ?: stringResource(R.string.place_object_hint),
                color = Color.White.copy(alpha = 0.85f),
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            )
        }

        // Center viewfinder brackets
        ViewfinderFrame(
            modifier = Modifier
                .align(Alignment.Center)
                .size(200.dp),
        )

        // Bottom control bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .navigationBarsPadding()
                .padding(vertical = 18.dp, horizontal = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = onOpenGallery,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape),
            ) {
                Icon(Icons.Outlined.PhotoLibrary, contentDescription = "Gallery")
            }

            RainbowShutter(
                onClick = {
                    imageCapture.takePicture(
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                val bmp = image.toUprightBitmap()
                                image.close()
                                flowViewModel.onCaptured(bmp)
                                onCaptured()
                            }

                            override fun onError(exception: ImageCaptureException) {
                                exception.printStackTrace()
                            }
                        },
                    )
                },
            )

            // Spacer matching the gallery button to keep the shutter centered.
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(44.dp))
        }
    }
}

private const val ANALYSIS_INTERVAL_MS = 700L
