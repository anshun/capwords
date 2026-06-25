package com.capwords.ui.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.camera.core.ImageProxy
import kotlin.math.cos
import kotlin.math.sin

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

    /** Crop a transparent cut-out to its subject's bounding box (+ small margin). */
    fun Bitmap.trimToAlpha(alphaThreshold: Int = 16, marginFraction: Float = 0.04f): Bitmap {
        val pixels = IntArray(width * height)
        getPixels(pixels, 0, width, 0, 0, width, height)
        var minX = width; var minY = height; var maxX = -1; var maxY = -1
        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                if ((pixels[row + x] ushr 24) > alphaThreshold) {
                    if (x < minX) minX = x; if (x > maxX) maxX = x
                    if (y < minY) minY = y; if (y > maxY) maxY = y
                }
            }
        }
        if (maxX < minX || maxY < minY) return this // nothing opaque
        val margin = (maxOf(maxX - minX, maxY - minY) * marginFraction).toInt()
        val l = (minX - margin).coerceAtLeast(0)
        val t = (minY - margin).coerceAtLeast(0)
        val r = (maxX + margin).coerceAtMost(width - 1)
        val b = (maxY + margin).coerceAtMost(height - 1)
        return Bitmap.createBitmap(this, l, t, r - l + 1, b - t + 1)
    }

    /**
     * Wrap a transparent cut-out in a white "die-cut" sticker border by stamping a
     * white silhouette around a circle of offsets, then the original on top.
     */
    fun Bitmap.withWhiteStickerBorder(borderPx: Int = 12): Bitmap {
        val pad = borderPx + 2
        val out = Bitmap.createBitmap(width + 2 * pad, height + 2 * pad, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        }
        val steps = 24
        for (i in 0 until steps) {
            val ang = 2.0 * Math.PI * i / steps
            val dx = (cos(ang) * borderPx).toFloat()
            val dy = (sin(ang) * borderPx).toFloat()
            canvas.drawBitmap(this, pad + dx, pad + dy, whitePaint)
        }
        canvas.drawBitmap(this, pad.toFloat(), pad.toFloat(), null)
        return out
    }
}
