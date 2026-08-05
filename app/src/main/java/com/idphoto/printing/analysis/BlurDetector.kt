package com.idphoto.printing.analysis

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.idphoto.printing.core.FloatRect
import com.idphoto.printing.core.ImageSize
import kotlin.math.roundToInt

data class BlurResult(
    val score: Double,
    val isSharp: Boolean,
)

object BlurDetector {
    private const val ANALYSIS_SIZE = 384
    // Conservative threshold: it is intended to reject obvious motion/focus blur,
    // not minor softness or normal skin texture differences between phones.
    internal const val MIN_LAPLACIAN_VARIANCE = 45.0

    fun analyze(
        bitmap: Bitmap,
        crop: FloatRect,
        originalSize: ImageSize,
    ): BlurResult {
        val scaleX = bitmap.width.toFloat() / originalSize.width.coerceAtLeast(1)
        val scaleY = bitmap.height.toFloat() / originalSize.height.coerceAtLeast(1)
        val source = Rect(
            (crop.left * scaleX).roundToInt().coerceIn(0, bitmap.width - 1),
            (crop.top * scaleY).roundToInt().coerceIn(0, bitmap.height - 1),
            (crop.right * scaleX).roundToInt().coerceIn(1, bitmap.width),
            (crop.bottom * scaleY).roundToInt().coerceIn(1, bitmap.height),
        )
        if (source.right <= source.left || source.bottom <= source.top) {
            return BlurResult(0.0, false)
        }

        val sample = Bitmap.createBitmap(
            ANALYSIS_SIZE,
            ANALYSIS_SIZE,
            Bitmap.Config.ARGB_8888,
        )
        try {
            Canvas(sample).drawBitmap(
                bitmap,
                source,
                RectF(0f, 0f, ANALYSIS_SIZE.toFloat(), ANALYSIS_SIZE.toFloat()),
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
            )
            val pixels = IntArray(ANALYSIS_SIZE * ANALYSIS_SIZE)
            sample.getPixels(pixels, 0, ANALYSIS_SIZE, 0, 0, ANALYSIS_SIZE, ANALYSIS_SIZE)
            val luminance = IntArray(pixels.size) { index ->
                val color = pixels[index]
                val red = color shr 16 and 0xff
                val green = color shr 8 and 0xff
                val blue = color and 0xff
                (77 * red + 150 * green + 29 * blue) shr 8
            }
            val score = laplacianVariance(luminance, ANALYSIS_SIZE, ANALYSIS_SIZE)
            return BlurResult(score, score >= MIN_LAPLACIAN_VARIANCE)
        } finally {
            sample.recycle()
        }
    }

    internal fun laplacianVariance(
        luminance: IntArray,
        width: Int,
        height: Int,
    ): Double {
        require(width >= 3 && height >= 3 && luminance.size == width * height)
        var sum = 0.0
        var sumOfSquares = 0.0
        var count = 0
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val index = y * width + x
                val laplacian = 4 * luminance[index] -
                    luminance[index - 1] - luminance[index + 1] -
                    luminance[index - width] - luminance[index + width]
                sum += laplacian
                sumOfSquares += laplacian.toDouble() * laplacian
                count++
            }
        }
        if (count == 0) return 0.0
        val mean = sum / count
        return (sumOfSquares / count - mean * mean).coerceAtLeast(0.0)
    }
}
