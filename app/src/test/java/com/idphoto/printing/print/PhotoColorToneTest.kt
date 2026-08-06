package com.idphoto.printing.print

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoColorToneTest {
    @Test
    fun neutralToneUsesIdentityColorMatrix() {
        assertArrayEquals(
            floatArrayOf(
                1f, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            ),
            PhotoColorTone.NEUTRAL.colorMatrixValues(),
            0.0001f,
        )
    }

    @Test
    fun directPrintToneIsWarmerAndPreservesAlpha() {
        val tone = PhotoColorTone.DIRECT_PRINT_WARM
        val matrix = tone.colorMatrixValues()

        assertTrue(tone.redScale > 1f)
        assertTrue(tone.greenScale >= 1f)
        assertTrue(tone.blueScale < 1f)
        assertTrue(tone.redScale / tone.blueScale > 1.1f)
        assertEquals(1f, matrix[18], 0.0001f)
        assertEquals(0f, matrix[19], 0.0001f)
    }
}
