package com.idphoto.printing.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.idphoto.printing.core.FloatRect
import com.idphoto.printing.core.ImageSize
import kotlin.math.roundToInt

@Composable
fun PhotoCrop(
    bitmap: Bitmap,
    crop: FloatRect,
    originalSize: ImageSize,
    modifier: Modifier = Modifier,
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    Canvas(modifier) {
        drawRect(Color.White)
        val scaleX = bitmap.width.toFloat() / originalSize.width.coerceAtLeast(1)
        val scaleY = bitmap.height.toFloat() / originalSize.height.coerceAtLeast(1)
        val left = (crop.left * scaleX).roundToInt().coerceIn(0, bitmap.width - 1)
        val top = (crop.top * scaleY).roundToInt().coerceIn(0, bitmap.height - 1)
        val right = (crop.right * scaleX).roundToInt().coerceIn(left + 1, bitmap.width)
        val bottom = (crop.bottom * scaleY).roundToInt().coerceIn(top + 1, bitmap.height)

        drawImage(
            image = imageBitmap,
            srcOffset = IntOffset(left, top),
            srcSize = IntSize(right - left, bottom - top),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.High,
        )
    }
}
