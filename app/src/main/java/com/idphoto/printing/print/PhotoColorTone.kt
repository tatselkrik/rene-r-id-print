package com.idphoto.printing.print

/** Color treatment for the photo cells only; page white and cut guides are separate. */
enum class PhotoColorTone(
    val redScale: Float,
    val greenScale: Float,
    val blueScale: Float,
) {
    NEUTRAL(
        redScale = 1f,
        greenScale = 1f,
        blueScale = 1f,
    ),
    DIRECT_PRINT_WARM(
        redScale = 1.06f,
        greenScale = 1.015f,
        blueScale = 0.94f,
    ),
    ;

    fun colorMatrixValues(): FloatArray = floatArrayOf(
        redScale, 0f, 0f, 0f, 0f,
        0f, greenScale, 0f, 0f, 0f,
        0f, 0f, blueScale, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )
}
