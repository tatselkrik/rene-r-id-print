package com.idphoto.printing.core

enum class PhotoKind {
    LARGE_SQUARE,
    PASSPORT_35X45,
    SMALL_SQUARE,
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

    const val OCCUPIED_WIDTH_MM =
        LARGE_MM + COLUMN_GAP_MM + PASSPORT_WIDTH_MM + COLUMN_GAP_MM + SMALL_MM
    const val SIDE_MARGIN_MM = (PAPER_WIDTH_MM - OCCUPIED_WIDTH_MM) / 2f
    const val LARGE_COLUMN_HEIGHT_MM = LARGE_MM * 3f + CUT_GAP_MM * 2f
    const val TOP_MARGIN_MM = (PAPER_HEIGHT_MM - LARGE_COLUMN_HEIGHT_MM) / 2f

    val cells: List<PhotoCell> = buildList {
        repeat(3) { index ->
            add(
                PhotoCell(
                    kind = PhotoKind.LARGE_SQUARE,
                    bounds = RectMm(
                        left = SIDE_MARGIN_MM,
                        top = TOP_MARGIN_MM + index * (LARGE_MM + CUT_GAP_MM),
                        width = LARGE_MM,
                        height = LARGE_MM,
                    ),
                ),
            )
        }

        val passportLeft = SIDE_MARGIN_MM + LARGE_MM + COLUMN_GAP_MM
        repeat(3) { index ->
            add(
                PhotoCell(
                    kind = PhotoKind.PASSPORT_35X45,
                    bounds = RectMm(
                        left = passportLeft,
                        top = TOP_MARGIN_MM + index * (PASSPORT_HEIGHT_MM + CUT_GAP_MM),
                        width = PASSPORT_WIDTH_MM,
                        height = PASSPORT_HEIGHT_MM,
                    ),
                ),
            )
        }

        val smallLeft = passportLeft + PASSPORT_WIDTH_MM + COLUMN_GAP_MM
        repeat(4) { index ->
            add(
                PhotoCell(
                    kind = PhotoKind.SMALL_SQUARE,
                    bounds = RectMm(
                        left = smallLeft,
                        top = TOP_MARGIN_MM + index * (SMALL_MM + CUT_GAP_MM),
                        width = SMALL_MM,
                        height = SMALL_MM,
                    ),
                ),
            )
        }
    }
}
