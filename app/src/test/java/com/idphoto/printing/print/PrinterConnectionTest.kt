package com.idphoto.printing.print

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PrinterConnectionTest {
    private val original = DirectPrinterProfile(host = "printer.test", certificateSha256 = "original-pin")

    @Test fun `startup checks saved printer with its pin without discovery`() = runBlocking {
        var stored: DirectPrinterProfile? = original
        val connection = PrinterConnection({ stored }, { stored = it }, { error("Unexpected discovery") }) { address, pin ->
            assertEquals(original.probeAddress(), address)
            assertEquals(original.certificateSha256, pin)
            original
        }
        connection.refresh()
        assertEquals(original, connection.state.value.profile)
        assertEquals("Printer ready", connection.state.value.message)
    }

    @Test fun `changed address keeps certificate and updates saved profile`() = runBlocking {
        var stored: DirectPrinterProfile? = original
        val moved = original.copy(host = "moved.test")
        val calls = mutableListOf<String?>()
        val connection = PrinterConnection({ stored }, { stored = it }, { moved.probeAddress() }) { address, pin ->
            calls += pin
            if (address == original.probeAddress()) throw IOException("Old address unavailable")
            moved
        }
        connection.refresh()
        assertEquals(listOf("original-pin", "original-pin"), calls)
        assertEquals(moved, stored)
        assertEquals(moved, connection.state.value.profile)
    }

    @Test fun `new printer is discovered and paired without blocking capture state`() = runBlocking {
        var stored: DirectPrinterProfile? = null
        val connection = PrinterConnection({ stored }, { stored = it }, { original.probeAddress() }) { _, pin ->
            assertNull(pin)
            original
        }
        connection.refresh()
        assertEquals(original, stored)
        assertFalse(connection.state.value.checking)
    }

    @Test fun `different certificate is never silently persisted`() = runBlocking {
        var stored: DirectPrinterProfile? = original
        val connection = PrinterConnection({ stored }, { stored = it }, { "other.test" }) { _, _ ->
            original.copy(certificateSha256 = "different-pin")
        }
        connection.refresh()
        assertEquals(original, stored)
        assertNull(connection.state.value.profile)
        assertTrue(connection.state.value.message.contains("certificate changed"))
    }

    @Test fun `legacy unencrypted profile requires explicit secure setup`() = runBlocking {
        val legacy = original.copy(useTls = false, certificateSha256 = null)
        val connection = PrinterConnection({ legacy }, { error("Unexpected save") }, { error("Unexpected discovery") }) { _, _ ->
            error("Unexpected probe")
        }
        connection.refresh()
        assertNull(connection.state.value.profile)
        assertTrue(connection.state.value.message.contains("Printer Setup"))
    }

    @Test fun `offline printer clears readiness and later refresh recovers`() = runBlocking {
        var online = false
        val connection = PrinterConnection({ original }, {}, { throw IOException("Printer unavailable") }) { _, _ ->
            if (!online) throw IOException("Offline")
            original
        }
        connection.refresh()
        assertNull(connection.state.value.profile)
        assertFalse(connection.state.value.checking)
        online = true
        connection.refresh()
        assertEquals(original, connection.state.value.profile)
    }

    @Test fun `ambiguous discovery cannot save a printer`() = runBlocking {
        val connection = PrinterConnection({ null }, { error("Unexpected save") }, { throw IOException("Several printers") }) { _, _ ->
            error("Unexpected probe")
        }
        connection.refresh()
        assertNull(connection.state.value.profile)
        assertEquals("Several printers", connection.state.value.message)
    }

    @Test fun `cancellation does not trigger discovery or save`() = runBlocking {
        val connection = PrinterConnection({ original }, { error("Unexpected save") }, { error("Unexpected discovery") }) { _, _ ->
            throw CancellationException("Stopped")
        }
        try {
            connection.refresh()
            fail("Cancellation must propagate")
        } catch (_: CancellationException) {
            assertFalse(connection.state.value.checking)
        }
    }

    @Test fun `incompatible profile cannot be saved by explicit setup`() = runBlocking {
        val connection = PrinterConnection({ null }, { error("Unexpected save") }, { "printer.test" }) { _, _ ->
            original.copy(supportsJpeg = false)
        }
        try {
            connection.connect("printer.test")
            fail("Incompatible printer must fail")
        } catch (_: IOException) {
            assertNull(connection.state.value.profile)
        }
    }
}
