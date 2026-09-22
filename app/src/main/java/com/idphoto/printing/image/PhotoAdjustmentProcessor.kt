package com.idphoto.printing.image

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import com.idphoto.printing.core.FloatRect
import com.idphoto.printing.core.ImageSize
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

object PhotoAdjustmentProcessor {
    fun automatic(bitmap: Bitmap, crop: FloatRect, originalSize: ImageSize): PhotoAdjustments {
        // Sample the central portrait area, excluding most background and clothing.
        val samples = IntArray(64 * 64)
        val sx = bitmap.width.toFloat() / originalSize.width
        val sy = bitmap.height.toFloat() / originalSize.height
        for (y in 0 until 64) for (x in 0 until 64) {
            val px = ((crop.left + crop.width * (0.25f + x / 63f * 0.5f)) * sx)
                .toInt().coerceIn(0, bitmap.width - 1)
            val py = ((crop.top + crop.height * (0.15f + y / 63f * 0.5f)) * sy)
                .toInt().coerceIn(0, bitmap.height - 1)
            samples[y * 64 + x] = bitmap[px, py]
        }
        return PhotoAdjustments.automatic(samples)
    }

    suspend fun apply(source: Bitmap, settings: PhotoAdjustments): Bitmap {
        if (settings == PhotoAdjustments.NEUTRAL) return source
        val output = createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        try {
            val row = IntArray(source.width)
            val coroutine = currentCoroutineContext()
            for (y in 0 until source.height) {
                coroutine.ensureActive()
                source.getPixels(row, 0, source.width, 0, y, source.width, 1)
                for (x in row.indices) row[x] = settings.apply(row[x])
                output.setPixels(row, 0, source.width, 0, y, source.width, 1)
            }
            return output
        } catch (error: Throwable) {
            output.recycle()
            throw error
        }
    }
}
