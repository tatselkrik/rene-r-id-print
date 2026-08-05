package com.idphoto.printing.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrintScaleTest {
    @Test
    fun `L15150 woven matte correction averages all measured sizes`() {
        assertEquals(1.0599f, PrintScale.L15150_RC_WOVEN_MATTE.x, 0.0002f)
        assertEquals(1.0594f, PrintScale.L15150_RC_WOVEN_MATTE.y, 0.0002f)
    }

    @Test
    fun `maximum correction keeps the sheet inside the page`() {
        val scale = PrintScale(PrintScale.MAX_SCALE, PrintScale.MAX_SCALE)
        SheetLayout.cells.forEach { cell ->
            val scaledLeft = scaleCoordinate(
                value = cell.bounds.left,
                center = SheetLayout.PAPER_WIDTH_MM / 2f,
                scale = scale.x,
            )
            val scaledRight = scaleCoordinate(
                value = cell.bounds.right,
                center = SheetLayout.PAPER_WIDTH_MM / 2f,
                scale = scale.x,
            )
            val scaledTop = scaleCoordinate(
                value = cell.bounds.top,
                center = SheetLayout.PAPER_HEIGHT_MM / 2f,
                scale = scale.y,
            )
            val scaledBottom = scaleCoordinate(
                value = cell.bounds.bottom,
                center = SheetLayout.PAPER_HEIGHT_MM / 2f,
                scale = scale.y,
            )

            assertTrue(scaledLeft >= 0f)
            assertTrue(scaledTop >= 0f)
            assertTrue(scaledRight <= SheetLayout.PAPER_WIDTH_MM)
            assertTrue(scaledBottom <= SheetLayout.PAPER_HEIGHT_MM)
        }
    }

    private fun scaleCoordinate(value: Float, center: Float, scale: Float): Float =
        center + (value - center) * scale
}
