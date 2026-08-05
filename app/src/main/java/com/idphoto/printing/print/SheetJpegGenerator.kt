package com.idphoto.printing.print

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.exifinterface.media.ExifInterface
import com.idphoto.printing.analysis.PhotoReview
import com.idphoto.printing.core.PrintScale
import java.io.File
import java.io.IOException

object SheetJpegGenerator {
    const val WIDTH_PIXELS = 5 * 600
    const val HEIGHT_PIXELS = 7 * 600
    private const val JPEG_QUALITY = 100

    fun generate(
        outputFile: File,
        bitmap: Bitmap,
        review: PhotoReview,
        printScale: PrintScale = PrintScale.IDENTITY,
    ): File {
        outputFile.parentFile?.mkdirs()
        val sheet = Bitmap.createBitmap(WIDTH_PIXELS, HEIGHT_PIXELS, Bitmap.Config.ARGB_8888)
        try {
            SheetCanvasRenderer.draw(
                canvas = Canvas(sheet),
                width = WIDTH_PIXELS.toFloat(),
                height = HEIGHT_PIXELS.toFloat(),
                bitmap = bitmap,
                review = review,
                printScale = printScale,
            )
            outputFile.outputStream().buffered().use { stream ->
                if (!sheet.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)) {
                    throw IOException("The 5 x 7 JPEG could not be created.")
                }
            }
            ExifInterface(outputFile).apply {
                setAttribute(ExifInterface.TAG_X_RESOLUTION, "600/1")
                setAttribute(ExifInterface.TAG_Y_RESOLUTION, "600/1")
                setAttribute(ExifInterface.TAG_RESOLUTION_UNIT, "2")
                saveAttributes()
            }
        } finally {
            sheet.recycle()
        }
        return outputFile
    }
}
