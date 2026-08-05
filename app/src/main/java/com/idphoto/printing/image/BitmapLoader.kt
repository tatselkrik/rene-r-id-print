package com.idphoto.printing.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import kotlin.math.max

object BitmapLoader {
    private const val MAX_DECODED_DIMENSION = 4096

    fun load(file: File): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)

        var sampleSize = 1
        while (max(bounds.outWidth, bounds.outHeight) / sampleSize > MAX_DECODED_DIMENSION) {
            sampleSize *= 2
        }

        val decoded = requireNotNull(
            BitmapFactory.decodeFile(
                file.absolutePath,
                BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                },
            ),
        ) { "The captured image could not be decoded." }

        val rotation = ExifInterface(file).rotationDegrees.toFloat()
        if (rotation == 0f) return decoded

        val rotated = Bitmap.createBitmap(
            decoded,
            0,
            0,
            decoded.width,
            decoded.height,
            Matrix().apply { postRotate(rotation) },
            true,
        )
        if (rotated !== decoded) decoded.recycle()
        return rotated
    }
}
