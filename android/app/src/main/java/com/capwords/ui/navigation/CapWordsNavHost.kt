package com.capwords.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.capwords.ui.camera.CameraScreen
import com.capwords.ui.capture.CaptureScreen
import com.capwords.ui.flow.CaptureFlowViewModel
import com.capwords.ui.gallery.GalleryScreen
import com.capwords.ui.recognize.RecognizeScreen
import com.capwords.ui.util.rememberSpeechHelper

object Routes {
    const val CAMERA = "camera"
    const val CAPTURE = "capture"
    const val RECOGNIZE = "recognize"
    const val GALLERY = "gallery"
}

@Composable
fun CapWordsNavHost() {
    val navController = rememberNavController()
    // Activity-scoped: the same instance is shared across the capture flow.
    val flowViewModel: CaptureFlowViewModel = viewModel()
    val speech = rememberSpeechHelper()

    NavHost(navController = navController, startDestination = Routes.CAMERA) {
        composable(Routes.CAMERA) {
            CameraScreen(
                flowViewModel = flowViewModel,
                onCaptured = { navController.navigate(Routes.CAPTURE) },
                onOpenGallery = { navController.navigate(Routes.GALLERY) },
            )
        }
        composable(Routes.CAPTURE) {
            CaptureScreen(
                flowViewModel = flowViewModel,
                onConfirmed = {
                    navController.navigate(Routes.RECOGNIZE) {
                        popUpTo(Routes.CAMERA)
                    }
                },
                onCancel = { navController.popBackStack(Routes.CAMERA, inclusive = false) },
            )
        }
        composable(Routes.RECOGNIZE) {
            RecognizeScreen(
                flowViewModel = flowViewModel,
                speech = speech,
                onSaved = {
                    navController.navigate(Routes.GALLERY) {
                        popUpTo(Routes.CAMERA)
                    }
                },
                onCancel = { navController.popBackStack(Routes.CAMERA, inclusive = false) },
            )
        }
        composable(Routes.GALLERY) {
            GalleryScreen(
                speech = speech,
                onBack = { navController.popBackStack(Routes.CAMERA, inclusive = false) },
                onAddNew = { navController.popBackStack(Routes.CAMERA, inclusive = false) },
            )
        }
    }
}
