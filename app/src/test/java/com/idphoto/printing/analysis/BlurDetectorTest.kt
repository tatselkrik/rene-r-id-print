package com.idphoto.printing.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlurDetectorTest {
    @Test
    fun `uniform image has no sharpness energy`() {
        val luminance = IntArray(8 * 8) { 128 }

        assertEquals(0.0, BlurDetector.laplacianVariance(luminance, 8, 8), 0.0001)
    }

    @Test
    fun `strong alternating edges exceed conservative sharpness threshold`() {
        val luminance = IntArray(8 * 8) { index ->
            val x = index % 8
            val y = index / 8
            if ((x + y) % 2 == 0) 0 else 255
        }
        val score = BlurDetector.laplacianVariance(luminance, 8, 8)

        assertTrue(score > BlurDetector.MIN_LAPLACIAN_VARIANCE)
    }
}
