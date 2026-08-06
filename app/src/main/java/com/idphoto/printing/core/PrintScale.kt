package com.idphoto.printing.core

data class PrintScale(
    val x: Float = 1f,
    val y: Float = 1f,
) {
    init {
        require(x in MIN_SCALE..MAX_SCALE) { "Horizontal print scale is outside the safe range." }
        require(y in MIN_SCALE..MAX_SCALE) { "Vertical print scale is outside the safe range." }
    }

    companion object {
        const val MIN_SCALE = 0.9f
        // Larger values would push the densest selectable layout beyond the 5 x 7 page.
        const val MAX_SCALE = 1.06f
        val IDENTITY = PrintScale()

        // Fixed L15150 / RC Woven matte correction averaged from the repeatable
        // measurements: 2 -> 1.875 in, 35x45 -> 33x42.5 mm, and 1 -> 0.95 in.
        val L15150_RC_WOVEN_MATTE = PrintScale(
            x = ((2f / 1.875f) + (35f / 33f) + (1f / 0.95f)) / 3f,
            y = ((2f / 1.875f) + (45f / 42.5f) + (1f / 0.95f)) / 3f,
        )
    }
}
