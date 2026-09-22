package com.idphoto.printing.image

import org.junit.Assert.*
import org.junit.Test

class PhotoAdjustmentsTest {
    private fun red(c: Int) = c ushr 16 and 255
    private fun green(c: Int) = c ushr 8 and 255
    private fun blue(c: Int) = c and 255

    @Test fun neutralPreservesPixelsExactly() {
        val colors = intArrayOf(0, -1, 0xff987654.toInt(), 0x80704020.toInt(), 0xff000000.toInt())
        colors.forEach { assertEquals(it, PhotoAdjustments.NEUTRAL.apply(it)) }
    }

    @Test fun alphaAndTransparentBackgroundArePreserved() {
        val settings = PhotoAdjustments(1f, -1f, 1f, 1f, -1f)
        assertEquals(0x00123456, settings.apply(0x00123456))
        for (alpha in 1..255) {
            assertEquals(alpha, settings.apply((alpha shl 24) or 0x987654) ushr 24)
        }
    }

    @Test fun brightnessMovesChannelsTogether() {
        val pixel = 0xff705030.toInt()
        val light = PhotoAdjustments(brightness = 0.5f).apply(pixel)
        val dark = PhotoAdjustments(brightness = -0.5f).apply(pixel)
        assertTrue(red(light) > red(pixel))
        assertTrue(red(dark) < red(pixel))
        assertEquals(red(pixel) - green(pixel), red(light) - green(light))
    }

    @Test fun contrastSeparatesShadowsAndHighlights() {
        val settings = PhotoAdjustments(contrast = 1f)
        assertTrue(red(settings.apply(0xff404040.toInt())) < 64)
        assertTrue(red(settings.apply(0xffc0c0c0.toInt())) > 192)
    }

    @Test fun saturationMinimumIsGrayscale() {
        val result = PhotoAdjustments(saturation = -1f).apply(0xffb07040.toInt())
        assertEquals(red(result), green(result))
        assertEquals(green(result), blue(result))
    }

    @Test fun temperatureMovesRedAndBlueInOppositeDirections() {
        val warm = PhotoAdjustments(temperature = 1f).apply(0xff808080.toInt())
        val cool = PhotoAdjustments(temperature = -1f).apply(0xff808080.toInt())
        assertTrue(red(warm) > blue(warm))
        assertTrue(red(cool) < blue(cool))
        assertEquals(128, green(warm))
    }

    @Test fun vibranceLeavesGrayAndFullySaturatedColorsAlone() {
        val settings = PhotoAdjustments(vibrance = 1f)
        assertEquals(0xff808080.toInt(), settings.apply(0xff808080.toInt()))
        assertEquals(0xffff0000.toInt(), settings.apply(0xffff0000.toInt()))
        val muted = 0xff809080.toInt()
        val result = settings.apply(muted)
        assertTrue(green(result) - red(result) > green(muted) - red(muted))
    }

    @Test fun extremeCombinedSettingsClampWithoutChangingAlpha() {
        for (v in listOf(-1f, 1f)) for (c in 0..255) {
            val result = PhotoAdjustments(v, v, v, v, v).apply(0x80000000.toInt() or (c * 0x010101))
            assertEquals(128, result ushr 24)
        }
    }

    @Test fun autoIgnoresTransparentAndWhiteBackgroundAndBoundsCorrection() {
        val subject = IntArray(100) { 0xff505050.toInt() }
        val correction = PhotoAdjustments.automatic(subject)
        assertEquals(correction, PhotoAdjustments.automatic(subject + IntArray(100) { -1 } + IntArray(100)))
        assertTrue(correction.brightness in 0f..0.24f)
        assertTrue(correction.contrast in 0f..0.12f)
        assertEquals(0f, correction.temperature, 0f)
        assertEquals(0f, correction.saturation, 0f)
    }

    @Test fun autoWithoutEnoughSubjectDataIsNeutral() {
        assertEquals(PhotoAdjustments.NEUTRAL, PhotoAdjustments.automatic(IntArray(200) { -1 }))
        assertEquals(PhotoAdjustments.NEUTRAL, PhotoAdjustments.automatic(intArrayOf(0xff555555.toInt())))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonFiniteSettings() { PhotoAdjustments(brightness = Float.NaN) }
}
