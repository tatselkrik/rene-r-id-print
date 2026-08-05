package com.idphoto.printing.print

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.idphoto.printing.analysis.PhotoReview
import com.idphoto.printing.core.FloatRect
import com.idphoto.printing.core.PhotoKind
import com.idphoto.printing.core.PrintScale
import com.idphoto.printing.core.SheetLayout
import kotlin.math.roundToInt

/** Draws the locked millimetre layout onto the preview/export/print JPEG bitmap. */
internal object SheetCanvasRenderer {
    fun draw(
        canvas: Canvas,
        width: Float,
        height: Float,
        bitmap: Bitmap,
        review: PhotoReview,
        printScale: PrintScale,
    ) {
        val cropPlan = requireNotNull(review.cropPlan) { "A valid crop plan is required." }
        val pixelsPerMmX = width / SheetLayout.PAPER_WIDTH_MM
        val pixelsPerMmY = height / SheetLayout.PAPER_HEIGHT_MM
        val photoPaint = Paint(
            Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG,
        )
        val guidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = SheetLayout.CUT_GUIDE_STROKE_MM * pixelsPerMmX
            strokeCap = Paint.Cap.SQUARE
        }

        canvas.drawColor(Color.WHITE)
        canvas.save()
        try {
            canvas.scale(printScale.x, printScale.y, width / 2f, height / 2f)
            SheetLayout.cells.forEach { cell ->
                val crop = when (cell.kind) {
                    PhotoKind.LARGE_SQUARE,
                    PhotoKind.SMALL_SQUARE -> cropPlan.square
                    PhotoKind.PASSPORT_35X45 -> cropPlan.passport
                }
                val destination = RectF(
                    cell.bounds.left * pixelsPerMmX,
                    cell.bounds.top * pixelsPerMmY,
                    cell.bounds.right * pixelsPerMmX,
                    cell.bounds.bottom * pixelsPerMmY,
                )
                canvas.drawBitmap(
                    bitmap,
                    crop.toBitmapRect(bitmap, review),
                    destination,
                    photoPaint,
                )
                drawCornerCutGuides(
                    canvas = canvas,
                    bounds = destination,
                    length = SheetLayout.CUT_GUIDE_LENGTH_MM * pixelsPerMmX,
                    paint = guidePaint,
                )
            }
        } finally {
            canvas.restore()
        }
    }

    private fun FloatRect.toBitmapRect(bitmap: Bitmap, review: PhotoReview): Rect {
        val scaleX = bitmap.width.toFloat() / review.imageSize.width.coerceAtLeast(1)
        val scaleY = bitmap.height.toFloat() / review.imageSize.height.coerceAtLeast(1)
        val leftPx = (left * scaleX).roundToInt().coerceIn(0, bitmap.width - 1)
        val topPx = (top * scaleY).roundToInt().coerceIn(0, bitmap.height - 1)
        val rightPx = (right * scaleX).roundToInt().coerceIn(leftPx + 1, bitmap.width)
        val bottomPx = (bottom * scaleY).roundToInt().coerceIn(topPx + 1, bitmap.height)
        return Rect(leftPx, topPx, rightPx, bottomPx)
    }

    private fun drawCornerCutGuides(
        canvas: Canvas,
        bounds: RectF,
        length: Float,
        paint: Paint,
    ) {
        val inset = paint.strokeWidth / 2f
        val left = bounds.left + inset
        val top = bounds.top + inset
        val right = bounds.right - inset
        val bottom = bounds.bottom - inset

        canvas.drawLine(left, top, left + length, top, paint)
        canvas.drawLine(left, top, left, top + length, paint)
        canvas.drawLine(right, top, right - length, top, paint)
        canvas.drawLine(right, top, right, top + length, paint)
        canvas.drawLine(left, bottom, left + length, bottom, paint)
        canvas.drawLine(left, bottom, left, bottom - length, paint)
        canvas.drawLine(right, bottom, right - length, bottom, paint)
        canvas.drawLine(right, bottom, right, bottom - length, paint)
    }
}
