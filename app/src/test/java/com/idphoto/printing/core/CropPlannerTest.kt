package com.idphoto.printing.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CropPlannerTest {
    @Test
    fun `camera square is preserved exactly for 2x2`() {
        val cameraSquare = FloatRect(900f, 1100f, 2100f, 2300f)
        val plan = CropPlanner.plan(
            image = ImageSize(3000, 4000),
            cameraSquare = cameraSquare,
        )

        assertEquals(cameraSquare, plan.square)
        assertTrue(plan.framingReliable)
        assertTrue(plan.resolutionSufficient)
    }

    @Test
    fun `passport keeps square height and crops only left and right`() {
        val plan = CropPlanner.plan(
            image = ImageSize(3000, 4000),
            cameraSquare = FloatRect(900f, 1100f, 2100f, 2300f),
        )

        assertEquals(plan.square.top, plan.passport.top, 0.001f)
        assertEquals(plan.square.bottom, plan.passport.bottom, 0.001f)
        assertEquals(plan.square.centerX, plan.passport.centerX, 0.001f)
        assertEquals(35f / 45f, plan.passport.width / plan.passport.height, 0.001f)
        assertTrue(plan.passport.left > plan.square.left)
        assertTrue(plan.passport.right < plan.square.right)
    }

    @Test
    fun `camera square outside saved image is rejected as unreliable`() {
        val plan = CropPlanner.plan(
            image = ImageSize(1200, 1600),
            cameraSquare = FloatRect(-10f, 200f, 790f, 1000f),
        )

        assertFalse(plan.framingReliable)
    }
}
