package com.idphoto.printing.core

import kotlin.math.min

data class ImageSize(val width: Int, val height: Int)

data class FloatRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

data class CropPlan(
    val square: FloatRect,
    val passport: FloatRect,
    val framingReliable: Boolean,
    val resolutionSufficient: Boolean,
)

object CropPlanner {
    private const val PASSPORT_WIDTH = 35f
    private const val PASSPORT_HEIGHT = 45f

    /**
     * Uses the square that was visible in the camera preview as the source of truth.
     * The passport crop keeps the square's complete height and removes only equal
     * strips from its left and right sides.
     */
    fun plan(image: ImageSize, cameraSquare: FloatRect): CropPlan {
        val requestedSide = min(cameraSquare.width, cameraSquare.height)
        val hasUsableInput = image.width > 0 && image.height > 0 && requestedSide > 0f
        val side = requestedSide.coerceAtMost(min(image.width, image.height).toFloat())
        val left = (cameraSquare.centerX - side / 2f).coerceIn(0f, image.width - side)
        val top = (cameraSquare.centerY - side / 2f).coerceIn(0f, image.height - side)
        val square = FloatRect(left, top, left + side, top + side)

        val passportWidth = square.height * PASSPORT_WIDTH / PASSPORT_HEIGHT
        val passportLeft = square.centerX - passportWidth / 2f
        val passport = FloatRect(
            left = passportLeft,
            top = square.top,
            right = passportLeft + passportWidth,
            bottom = square.bottom,
        )

        val stayedInsideImage =
            cameraSquare.left >= 0f &&
                cameraSquare.top >= 0f &&
                cameraSquare.right <= image.width &&
                cameraSquare.bottom <= image.height

        val resolutionSufficient =
            square.width >= 600f &&
                square.height >= 600f &&
                passport.width >= 414f &&
                passport.height >= 532f

        return CropPlan(
            square = square,
            passport = passport,
            framingReliable = hasUsableInput && stayedInsideImage,
            resolutionSufficient = resolutionSufficient,
        )
    }
}
