package com.idphoto.printing.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SheetLayoutTest {
    @Test
    fun `layout contains exactly the required copies`() {
        assertEquals(3, SheetLayout.cells.count { it.kind == PhotoKind.LARGE_SQUARE })
        assertEquals(3, SheetLayout.cells.count { it.kind == PhotoKind.PASSPORT_35X45 })
        assertEquals(4, SheetLayout.cells.count { it.kind == PhotoKind.SMALL_SQUARE })
        assertEquals(10, SheetLayout.cells.size)
    }

    @Test
    fun `all photos fit inside portrait five by seven sheet`() {
        assertTrue(SheetLayout.PAPER_HEIGHT_MM > SheetLayout.PAPER_WIDTH_MM)
        SheetLayout.cells.forEach { cell ->
            assertTrue(cell.bounds.left >= 0f)
            assertTrue(cell.bounds.top >= 0f)
            assertTrue(cell.bounds.right <= SheetLayout.PAPER_WIDTH_MM)
            assertTrue(cell.bounds.bottom <= SheetLayout.PAPER_HEIGHT_MM)
        }
    }

    @Test
    fun `finished photo dimensions remain exact`() {
        SheetLayout.cells.forEach { cell ->
            when (cell.kind) {
                PhotoKind.LARGE_SQUARE -> {
                    assertEquals(50.8f, cell.bounds.width, 0.001f)
                    assertEquals(50.8f, cell.bounds.height, 0.001f)
                }
                PhotoKind.PASSPORT_35X45 -> {
                    assertEquals(35f, cell.bounds.width, 0.001f)
                    assertEquals(45f, cell.bounds.height, 0.001f)
                }
                PhotoKind.SMALL_SQUARE -> {
                    assertEquals(25.4f, cell.bounds.width, 0.001f)
                    assertEquals(25.4f, cell.bounds.height, 0.001f)
                }
            }
        }
    }

    @Test
    fun `column and cutting gaps match the specification`() {
        val large = SheetLayout.cells.filter { it.kind == PhotoKind.LARGE_SQUARE }
        val passport = SheetLayout.cells.filter { it.kind == PhotoKind.PASSPORT_35X45 }
        val small = SheetLayout.cells.filter { it.kind == PhotoKind.SMALL_SQUARE }

        assertEquals(SheetLayout.SIDE_MARGIN_MM, large.first().bounds.left, 0.001f)
        assertEquals(
            SheetLayout.COLUMN_GAP_MM,
            passport.first().bounds.left - large.first().bounds.right,
            0.001f,
        )
        assertEquals(
            SheetLayout.COLUMN_GAP_MM,
            small.first().bounds.left - passport.first().bounds.right,
            0.001f,
        )
        assertEquals(
            SheetLayout.SIDE_MARGIN_MM,
            SheetLayout.PAPER_WIDTH_MM - small.first().bounds.right,
            0.001f,
        )

        listOf(large, passport, small).forEach { column ->
            column.zipWithNext().forEach { (upper, lower) ->
                assertEquals(
                    SheetLayout.CUT_GAP_MM,
                    lower.bounds.top - upper.bounds.bottom,
                    0.001f,
                )
            }
        }
    }

    @Test
    fun `physical units convert to exact 72 per inch reference units`() {
        assertEquals(72f, PhysicalUnits.mmToPoints(25.4f), 0.001f)
        assertEquals(144f, PhysicalUnits.mmToPoints(50.8f), 0.001f)
        assertEquals(360f, PhysicalUnits.mmToPoints(SheetLayout.PAPER_WIDTH_MM), 0.001f)
        assertEquals(504f, PhysicalUnits.mmToPoints(SheetLayout.PAPER_HEIGHT_MM), 0.001f)
    }

    @Test
    fun `corner cut guides stay short and visible`() {
        assertTrue(SheetLayout.CUT_GUIDE_LENGTH_MM > 0f)
        assertTrue(SheetLayout.CUT_GUIDE_STROKE_MM > 0f)
        assertTrue(SheetLayout.CUT_GUIDE_LENGTH_MM < SheetLayout.SMALL_MM / 2f)
    }
}
