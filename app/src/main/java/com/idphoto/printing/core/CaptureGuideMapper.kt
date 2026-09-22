package com.idphoto.printing.core

import kotlin.math.abs

/** Maps the shared CameraX viewport into the upright JPEG, independent of sensor rotation. */
object CaptureGuideMapper {
    fun uprightSize(encoded: ImageSize, rotationDegrees: Int): ImageSize {
        require(encoded.width > 0 && encoded.height > 0)
        require(rotationDegrees in listOf(0, 90, 180, 270))
        return if (rotationDegrees == 90 || rotationDegrees == 270)
            ImageSize(encoded.height, encoded.width) else encoded
    }

    fun map(preview: ImageSize, upright: ImageSize, guide: FloatRect): FloatRect {
        require(preview.width > 0 && preview.height > 0 && upright.width > 0 && upright.height > 0)
        val sx = upright.width.toFloat() / preview.width
        val sy = upright.height.toFloat() / preview.height
        // Permit only pixel-rounding differences, never turn an aspect mismatch into a tighter crop.
        require(abs(sx / sy - 1f) < 0.005f) {
            "The camera preview and photo proportions differ. Reopen the camera and try again."
        }
        require(listOf(guide.left, guide.top, guide.right, guide.bottom).all { it.isFinite() })
        require(guide.width > 0f && guide.height > 0f && abs(guide.width - guide.height) < 1f)
        require(guide.left >= 0 && guide.top >= 0 && guide.right <= preview.width && guide.bottom <= preview.height)
        val side = guide.width * sx
        val cx = guide.centerX * sx
        val cy = guide.centerY * sy
        return FloatRect(cx - side / 2f, cy - side / 2f, cx + side / 2f, cy + side / 2f)
    }
}
