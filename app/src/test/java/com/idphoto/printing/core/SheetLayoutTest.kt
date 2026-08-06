package com.idphoto.printing.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SheetLayoutTest {
    @Test
    fun `selector contains the eight approved maximum-use even combinations`() {
        val actual = SheetLayout.combinations.map {
            Triple(it.largeCount, it.passportCount, it.smallCount)
        }

        assertEquals(
            listOf(
                Triple(6, 0, 0),
                Triple(4, 2, 4),
                Triple(4, 0, 8),
                Triple(2, 6, 0),
                Triple(2, 4, 6),
                Triple(2, 2, 8),
                Triple(0, 8, 4),
                Triple(0, 6, 8),
            ),
            actual,
        )
    }

    @Test
    fun `four two four is the default combination`() {
        assertEquals(
            Triple(4, 2, 4),
            Triple(
                SheetCombination.DEFAULT.largeCount,
                SheetCombination.DEFAULT.passportCount,
                SheetCombination.DEFAULT.smallCount,
            ),
        )
    }

    @Test
    fun `every combination contains its advertised copies`() {
        SheetLayout.combinations.forEach { combination ->
            val cells = SheetLayout.cellsFor(combination)
            assertEquals(
                combination.largeCount,
                cells.count { it.kind == PhotoKind.LARGE_SQUARE },
            )
            assertEquals(
                combination.passportCount,
                cells.count { it.kind == PhotoKind.PASSPORT_35X45 },
            )
            assertEquals(
                combination.smallCount,
                cells.count { it.kind == PhotoKind.SMALL_SQUARE },
            )
            assertEquals(combination.totalCount, cells.size)
        }
    }

    @Test
    fun `all selectable quantities are zero or positive even numbers`() {
        SheetLayout.combinations.forEach { combination ->
            listOf(
                combination.largeCount,
                combination.passportCount,
                combination.smallCount,
            ).forEach { count ->
                assertTrue(count == 0 || count >= 2)
                assertEquals(0, count % 2)
            }
            assertTrue(combination.largeCount <= 6)
            assertTrue(combination.passportCount <= 8)
            assertTrue(combination.smallCount <= 8)
        }
    }

    @Test
    fun `all photos fit inside the portrait five by seven sheet`() {
        assertTrue(SheetLayout.PAPER_HEIGHT_MM > SheetLayout.PAPER_WIDTH_MM)
        SheetLayout.combinations.forEach { combination ->
            SheetLayout.cellsFor(combination).forEach { cell ->
                assertTrue(cell.bounds.left >= 0f)
                assertTrue(cell.bounds.top >= 0f)
                assertTrue(cell.bounds.right <= SheetLayout.PAPER_WIDTH_MM)
                assertTrue(cell.bounds.bottom <= SheetLayout.PAPER_HEIGHT_MM)
            }
        }
    }

    @Test
    fun `finished photo dimensions remain exact in every combination`() {
        SheetLayout.combinations.forEach { combination ->
            SheetLayout.cellsFor(combination).forEach { cell ->
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
    }

    @Test
    fun `every pair of photos retains a cutting gap`() {
        SheetLayout.combinations.forEach { combination ->
            val cells = SheetLayout.cellsFor(combination)
            cells.forEachIndexed { index, first ->
                cells.drop(index + 1).forEach { second ->
                    val separated =
                        first.bounds.right + SheetLayout.COLUMN_GAP_MM <=
                            second.bounds.left + TOLERANCE ||
                            second.bounds.right + SheetLayout.COLUMN_GAP_MM <=
                            first.bounds.left + TOLERANCE ||
                            first.bounds.bottom + SheetLayout.CUT_GAP_MM <=
                            second.bounds.top + TOLERANCE ||
                            second.bounds.bottom + SheetLayout.CUT_GAP_MM <=
                            first.bounds.top + TOLERANCE
                    assertTrue(
                        "${combination.name} contains cells without the required cutting gap: " +
                            "${first.bounds} and ${second.bounds}",
                        separated,
                    )
                }
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

    private companion object {
        const val TOLERANCE = 0.01f
    }
}
