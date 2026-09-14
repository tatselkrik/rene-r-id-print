package com.idphoto.printing.print

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectPrinterProfileTest {
    @org.junit.Test
    fun `plaintext and unpinned connections can never print`() {
        org.junit.Assert.assertFalse(DirectPrinterProfile("printer.test", useTls = false).readyForDirectPrint)
        org.junit.Assert.assertFalse(DirectPrinterProfile("printer.test").readyForDirectPrint)
        org.junit.Assert.assertTrue(DirectPrinterProfile("printer.test", certificateSha256 = "pin").readyForDirectPrint)
    }

    @org.junit.Test
    fun `printer model must match L15150 exactly`() {
        org.junit.Assert.assertTrue(isL15150Printer("EPSON L15150 Series", "Office"))
        org.junit.Assert.assertTrue(isL15150Printer("", "EPSON L15150 Series"))
        org.junit.Assert.assertFalse(isL15150Printer("EPSON L15160", "Office"))
        org.junit.Assert.assertFalse(isL15150Printer("", "Some other printer"))
        org.junit.Assert.assertFalse(isL15150Printer("EPSON L151500", "Office"))
    }
    @Test
    fun `direct profile defaults to cassette one and matte photo media`() {
        val profile = DirectPrinterProfile(host = "192.168.1.25")

        assertEquals("main", profile.mediaSource)
        assertEquals("photographic-matte", profile.mediaType)
        assertEquals("na_5x7_5x7in", profile.mediaKeyword)
        assertEquals(3000, SheetJpegGenerator.WIDTH_PIXELS)
        assertEquals(4200, SheetJpegGenerator.HEIGHT_PIXELS)
        assertFalse(profile.readyForDirectPrint)
        assertTrue(profile.copy(certificateSha256 = "test-certificate").readyForDirectPrint)
        assertFalse(
            profile.copy(
                certificateSha256 = "test-certificate",
                supportsJpeg = false,
            ).readyForDirectPrint,
        )
        assertFalse(
            profile.copy(
                certificateSha256 = "test-certificate",
                reportedFiveBySeven = false,
            ).readyForDirectPrint,
        )
    }
}
