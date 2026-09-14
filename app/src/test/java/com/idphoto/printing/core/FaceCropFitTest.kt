package com.idphoto.printing.core

import com.idphoto.printing.analysis.PhotoReview
import org.junit.Assert.*
import org.junit.Test

class FaceCropFitTest {
    private val plan = CropPlanner.plan(ImageSize(1000, 1000), FloatRect(100f, 100f, 900f, 900f))
    private val face = FloatRect(350f, 250f, 650f, 650f)
    private val leftEye = ImagePoint(420f, 380f)
    private val rightEye = ImagePoint(580f, 380f)

    @Test fun `centered face fits both crops`() {
        assertTrue(FaceCropFit.fits(plan.square, face, leftEye, rightEye))
        assertTrue(FaceCropFit.fits(plan.passport, face, leftEye, rightEye))
    }

    @Test fun `face visible in source but outside square fails`() {
        assertFalse(FaceCropFit.fits(plan.square, FloatRect(10f, 250f, 310f, 650f), leftEye, rightEye))
    }

    @Test fun `face clipped only by passport crop still permits square layouts`() {
        val offCenter = FloatRect(110f, 250f, 410f, 650f)
        val eye1 = ImagePoint(180f, 380f)
        val eye2 = ImagePoint(340f, 380f)
        assertTrue(FaceCropFit.fits(plan.square, offCenter, eye1, eye2))
        assertFalse(FaceCropFit.fits(plan.passport, offCenter, eye1, eye2))
        val review = PhotoReview(ImageSize(1000, 1000), plan, emptyList(), true, passportFaceFits = false)
        SheetCombination.entries.forEach {
            assertEquals(it.passportCount == 0, review.readyFor(it))
        }
    }

    @Test fun `missing and out of crop eyes fail`() {
        assertFalse(FaceCropFit.fits(plan.square, face, null, rightEye))
        assertFalse(FaceCropFit.fits(plan.square, face, ImagePoint(950f, 380f), rightEye))
    }

    @Test fun `nonfinite and inverted face boxes fail`() {
        assertFalse(FaceCropFit.fits(plan.square, face.copy(left = Float.NaN), leftEye, rightEye))
        assertFalse(FaceCropFit.fits(plan.square, face.copy(right = 300f), leftEye, rightEye))
        assertFalse(FaceCropFit.fits(plan.square, face, leftEye.copy(x = Float.POSITIVE_INFINITY), rightEye))
    }

    @Test fun `other failed checks block every combination even when passport fits`() {
        val review = PhotoReview(ImageSize(1000, 1000), plan, emptyList(), false, passportFaceFits = true)
        SheetCombination.entries.forEach { assertFalse(review.readyFor(it)) }
    }
}
