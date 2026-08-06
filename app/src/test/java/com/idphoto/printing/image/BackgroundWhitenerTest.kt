package com.idphoto.printing.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundWhitenerTest {
    @Test
    fun definiteBackgroundBecomesPureWhite() {
        val retention = BackgroundWhiteningMath.foregroundRetention(0f)

        assertEquals(0f, retention, 0.0001f)
        assertEquals(255, BackgroundWhiteningMath.blendWithWhite(40, retention))
    }

    @Test
    fun definitePersonKeepsOriginalColor() {
        val retention = BackgroundWhiteningMath.foregroundRetention(1f)

        assertEquals(1f, retention, 0.0001f)
        assertEquals(73, BackgroundWhiteningMath.blendWithWhite(73, retention))
    }

    @Test
    fun uncertainEdgesAreFeathered() {
        val retention = BackgroundWhiteningMath.foregroundRetention(0.35f)
        val blended = BackgroundWhiteningMath.blendWithWhite(80, retention)

        assertTrue(retention > 0f && retention < 1f)
        assertTrue(blended > 80 && blended < 255)
    }

    @Test
    fun retentionIncreasesWithPersonConfidence() {
        val values = listOf(0f, 0.2f, 0.4f, 0.6f, 1f)
            .map(BackgroundWhiteningMath::foregroundRetention)

        assertTrue(values.zipWithNext().all { (left, right) -> left <= right })
    }
}
