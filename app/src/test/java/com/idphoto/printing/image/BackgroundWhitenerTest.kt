package com.idphoto.printing.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundWhitenerTest {
    @Test
    fun definiteBackgroundBecomesPureWhite() {
        val retention = BackgroundWhiteningMath.foregroundRetention(0f)

        assertEquals(0f, retention, 0.0001f)
        assertEquals(0, BackgroundWhiteningMath.foregroundAlpha(retention))
    }

    @Test
    fun definitePersonRemainsFullyOpaque() {
        val retention = BackgroundWhiteningMath.foregroundRetention(1f)

        assertEquals(1f, retention, 0.0001f)
        assertEquals(255, BackgroundWhiteningMath.foregroundAlpha(retention))
    }

    @Test
    fun uncertainEdgesAreFeathered() {
        val retention = BackgroundWhiteningMath.foregroundRetention(0.35f)
        val alpha = BackgroundWhiteningMath.foregroundAlpha(retention)

        assertTrue(retention > 0f && retention < 1f)
        assertTrue(alpha in 1..254)
    }

    @Test
    fun retentionIncreasesWithPersonConfidence() {
        val values = listOf(0f, 0.2f, 0.4f, 0.6f, 1f)
            .map(BackgroundWhiteningMath::foregroundRetention)

        assertTrue(values.zipWithNext().all { (left, right) -> left <= right })
    }

    @Test
    fun subjectShadowsAreLiftedMoreThanHighlights() {
        val shadowGain = BackgroundWhiteningMath.subjectLighteningGain(
            red = 75,
            green = 60,
            blue = 55,
            foregroundRetention = 1f,
        )
        val highlightGain = BackgroundWhiteningMath.subjectLighteningGain(
            red = 225,
            green = 220,
            blue = 215,
            foregroundRetention = 1f,
        )

        assertTrue(shadowGain > highlightGain)
        assertTrue(highlightGain >= 1f)
        assertTrue(shadowGain <= 1.28f)
    }

    @Test
    fun definiteBackgroundDoesNotReceiveSubjectLightening() {
        val gain = BackgroundWhiteningMath.subjectLighteningGain(
            red = 60,
            green = 60,
            blue = 60,
            foregroundRetention = 0f,
        )

        assertEquals(1f, gain, 0.0001f)
    }

    @Test
    fun pureWhiteHighlightsStayPureWhite() {
        val gain = BackgroundWhiteningMath.subjectLighteningGain(
            red = 255,
            green = 255,
            blue = 255,
            foregroundRetention = 1f,
        )

        assertEquals(1f, gain, 0.0001f)
        assertEquals(255, BackgroundWhiteningMath.applyGain(255, gain))
    }
}
