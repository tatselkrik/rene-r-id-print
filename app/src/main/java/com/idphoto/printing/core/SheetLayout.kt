package com.idphoto.printing.core

enum class PhotoKind {
    LARGE_SQUARE,
    PASSPORT_35X45,
    SMALL_SQUARE,
}

enum class SheetCombination(
    val largeCount: Int,
    val passportCount: Int,
    val smallCount: Int,
) {
    SIX_LARGE(largeCount = 6, passportCount = 0, smallCount = 0),
    FOUR_LARGE_TWO_PASSPORT_FOUR_SMALL(
        largeCount = 4,
        passportCount = 2,
        smallCount = 4,
    ),
    FOUR_LARGE_EIGHT_SMALL(largeCount = 4, passportCount = 0, smallCount = 8),
    TWO_LARGE_SIX_PASSPORT(largeCount = 2, passportCount = 6, smallCount = 0),
    TWO_LARGE_FOUR_PASSPORT_SIX_SMALL(
        largeCount = 2,
        passportCount = 4,
        smallCount = 6,
    ),
    TWO_LARGE_TWO_PASSPORT_EIGHT_SMALL(
        largeCount = 2,
        passportCount = 2,
        smallCount = 8,
    ),
    EIGHT_PASSPORT_FOUR_SMALL(largeCount = 0, passportCount = 8, smallCount = 4),
    SIX_PASSPORT_EIGHT_SMALL(largeCount = 0, passportCount = 6, smallCount = 8),
    ;

    val totalCount: Int get() = largeCount + passportCount + smallCount

    companion object {
        val DEFAULT = FOUR_LARGE_TWO_PASSPORT_FOUR_SMALL
    }
}

data class RectMm(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height
}

data class PhotoCell(
    val kind: PhotoKind,
    val bounds: RectMm,
)

object SheetLayout {
    const val PAPER_WIDTH_MM = 127f
    const val PAPER_HEIGHT_MM = 177.8f
    const val COLUMN_GAP_MM = 3f
    const val CUT_GAP_MM = 2f
    const val CUT_GUIDE_LENGTH_MM = 3f
    const val CUT_GUIDE_STROKE_MM = 0.3f

    const val LARGE_MM = 50.8f
    const val PASSPORT_WIDTH_MM = 35f
    const val PASSPORT_HEIGHT_MM = 45f
    const val SMALL_MM = 25.4f

    val combinations: List<SheetCombination> = SheetCombination.entries

    fun cellsFor(combination: SheetCombination): List<PhotoCell> = when (combination) {
        SheetCombination.SIX_LARGE -> listOf(
            large(11.2f, 10.7f),
            large(65f, 10.7f),
            large(11.2f, 63.5f),
            large(65f, 63.5f),
            large(11.2f, 116.3f),
            large(65f, 116.3f),
        )

        SheetCombination.FOUR_LARGE_TWO_PASSPORT_FOUR_SMALL -> listOf(
            large(4.9f, 8.7f),
            large(58.7f, 8.7f),
            large(4.9f, 61.5f),
            small(58.7f, 61.5f),
            passport(87.1f, 61.5f),
            small(58.7f, 88.9f),
            passport(87.1f, 108.5f),
            large(4.9f, 114.3f),
            small(58.7f, 116.3f),
            small(58.7f, 143.7f),
        )

        SheetCombination.FOUR_LARGE_EIGHT_SMALL -> listOf(
            large(9.7f, 8.7f),
            large(63.5f, 8.7f),
            large(9.7f, 61.5f),
            small(63.5f, 61.5f),
            small(91.9f, 61.5f),
            small(63.5f, 88.9f),
            small(91.9f, 88.9f),
            large(9.7f, 114.3f),
            small(63.5f, 116.3f),
            small(91.9f, 116.3f),
            small(63.5f, 143.7f),
            small(91.9f, 143.7f),
        )

        SheetCombination.TWO_LARGE_SIX_PASSPORT -> listOf(
            large(8f, 16.5f),
            large(61.8f, 16.5f),
            passport(8f, 69.3f),
            passport(46f, 69.3f),
            passport(84f, 69.3f),
            passport(8f, 116.3f),
            passport(46f, 116.3f),
            passport(84f, 116.3f),
        )

        SheetCombination.TWO_LARGE_FOUR_PASSPORT_SIX_SMALL -> listOf(
            passport(4.9f, 7.7f),
            small(42.9f, 7.7f),
            passport(71.3f, 7.7f),
            small(42.9f, 35.1f),
            passport(4.9f, 54.7f),
            large(71.3f, 54.7f),
            small(42.9f, 62.5f),
            small(42.9f, 89.9f),
            passport(4.9f, 107.5f),
            large(71.3f, 107.5f),
            small(42.9f, 117.3f),
            small(42.9f, 144.7f),
        )

        SheetCombination.TWO_LARGE_TWO_PASSPORT_EIGHT_SMALL -> listOf(
            small(11.2f, 8.7f),
            small(39.6f, 8.7f),
            passport(68f, 8.7f),
            small(11.2f, 36.1f),
            small(39.6f, 36.1f),
            large(11.2f, 63.5f),
            large(65f, 63.5f),
            small(11.2f, 116.3f),
            small(39.6f, 116.3f),
            passport(68f, 116.3f),
            small(11.2f, 143.7f),
            small(39.6f, 143.7f),
        )

        SheetCombination.EIGHT_PASSPORT_FOUR_SMALL -> listOf(
            passport(8f, 5.7f),
            passport(46f, 5.7f),
            passport(84f, 5.7f),
            passport(8f, 52.7f),
            passport(46f, 52.7f),
            passport(84f, 52.7f),
            passport(27f, 99.7f),
            passport(65f, 99.7f),
            small(8.2f, 146.7f),
            small(36.6f, 146.7f),
            small(65f, 146.7f),
            small(93.4f, 146.7f),
        )

        SheetCombination.SIX_PASSPORT_EIGHT_SMALL -> listOf(
            passport(8f, 15.5f),
            passport(46f, 15.5f),
            passport(84f, 15.5f),
            passport(8f, 62.5f),
            passport(46f, 62.5f),
            passport(84f, 62.5f),
            small(8.2f, 109.5f),
            small(36.6f, 109.5f),
            small(65f, 109.5f),
            small(93.4f, 109.5f),
            small(8.2f, 136.9f),
            small(36.6f, 136.9f),
            small(65f, 136.9f),
            small(93.4f, 136.9f),
        )
    }

    private fun large(left: Float, top: Float) = PhotoCell(
        kind = PhotoKind.LARGE_SQUARE,
        bounds = RectMm(left, top, LARGE_MM, LARGE_MM),
    )

    private fun passport(left: Float, top: Float) = PhotoCell(
        kind = PhotoKind.PASSPORT_35X45,
        bounds = RectMm(left, top, PASSPORT_WIDTH_MM, PASSPORT_HEIGHT_MM),
    )

    private fun small(left: Float, top: Float) = PhotoCell(
        kind = PhotoKind.SMALL_SQUARE,
        bounds = RectMm(left, top, SMALL_MM, SMALL_MM),
    )
}
