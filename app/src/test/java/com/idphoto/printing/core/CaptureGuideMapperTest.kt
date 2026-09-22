package com.idphoto.printing.core

import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureGuideMapperTest {
    private val preview = ImageSize(1080, 1440)
    private val guide = FloatRect(151.2f, 331.2f, 928.8f, 1108.8f)

    @Test fun physicallyRotatedJpegAndExifRotatedJpegProduceSameCrop() {
        val expected = CaptureGuideMapper.map(preview, ImageSize(3000, 4000), guide)
        for (rotation in listOf(90, 270)) {
            val upright = CaptureGuideMapper.uprightSize(ImageSize(4000, 3000), rotation)
            assertEquals(expected, CaptureGuideMapper.map(preview, upright, guide))
        }
        for (rotation in listOf(0, 180)) {
            assertEquals(expected, CaptureGuideMapper.map(preview,
                CaptureGuideMapper.uprightSize(ImageSize(3000, 4000), rotation), guide))
        }
        assertEquals(420f, expected.left, 0.01f)
        assertEquals(920f, expected.top, 0.01f)
        assertEquals(2160f, expected.width, 0.01f)
    }

    @Test fun guideKeepsSameFractionAcrossCaptureResolutions() {
        for (size in listOf(ImageSize(1536, 2048), ImageSize(3000, 4000), ImageSize(6000, 8000))) {
            val crop = CaptureGuideMapper.map(preview, size, guide)
            assertEquals(0.72f, crop.width / size.width, 0.0001f)
            assertEquals(guide.top / preview.height, crop.top / size.height, 0.0001f)
        }
    }

    @Test fun displayDensityDoesNotChangeFraming() {
        val small = CaptureGuideMapper.map(ImageSize(540, 720), ImageSize(3000, 4000),
            FloatRect(75.6f, 165.6f, 464.4f, 554.4f))
        assertEquals(CaptureGuideMapper.map(preview, ImageSize(3000, 4000), guide), small)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mismatchedViewportIsRejectedInsteadOfShrinking() {
        CaptureGuideMapper.map(preview, ImageSize(3000, 3000), guide)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidImageSizeIsRejected() {
        CaptureGuideMapper.uprightSize(ImageSize(-1, -1), 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidGuideIsRejected() {
        CaptureGuideMapper.map(preview, ImageSize(3000, 4000), guide.copy(top = Float.NaN))
    }
}
