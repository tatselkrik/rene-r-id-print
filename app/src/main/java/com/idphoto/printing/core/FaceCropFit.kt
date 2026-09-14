package com.idphoto.printing.core

data class ImagePoint(val x: Float, val y: Float)

/** Basic clipping check, not an issuing authority's head-size or hair rule. */
object FaceCropFit {
    fun fits(
        crop: FloatRect,
        face: FloatRect?,
        leftEye: ImagePoint?,
        rightEye: ImagePoint?,
    ): Boolean {
        if (face == null || leftEye == null || rightEye == null) return false
        if (!crop.valid() || !face.valid()) return false
        return face.left >= crop.left && face.top >= crop.top &&
            face.right <= crop.right && face.bottom <= crop.bottom &&
            crop.contains(leftEye) && crop.contains(rightEye)
    }

    private fun FloatRect.valid(): Boolean =
        listOf(left, top, right, bottom).all { it.isFinite() } && width > 0f && height > 0f

    private fun FloatRect.contains(point: ImagePoint): Boolean =
        point.x.isFinite() && point.y.isFinite() &&
            point.x in left..right && point.y in top..bottom
}
