package com.capwords.ui.util

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy

object BitmapUtils {

    /** Convert a CameraX frame to an upright RGB bitmap. */
    fun ImageProxy.toUprightBitmap(): Bitmap {
        val bmp = toBitmap()
        val degrees = imageInfo.rotationDegrees
        return if (degrees == 0) bmp else bmp.rotate(degrees.toFloat())
    }

    fun Bitmap.rotate(degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    /** Downscale for the live analyzer to keep inference cheap. */
    fun Bitmap.scaledForAnalysis(maxDim: Int = 384): Bitmap {
        val longest = maxOf(width, height)
        if (longest <= maxDim) return this
        val scale = maxDim.toFloat() / longest
        return Bitmap.createScaledBitmap(this, (width * scale).toInt(), (height * scale).toInt(), true)
    }

    /** Center-square crop (used to align with the viewfinder framing). */
    fun Bitmap.centerSquare(): Bitmap {
        val side = minOf(width, height)
        val x = (width - side) / 2
        val y = (height - side) / 2
        return Bitmap.createBitmap(this, x, y, side, side)
    }
}
