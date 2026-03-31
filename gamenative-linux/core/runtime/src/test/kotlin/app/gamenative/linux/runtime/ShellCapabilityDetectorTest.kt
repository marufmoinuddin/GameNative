package app.gamenative.linux.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShellCapabilityDetectorTest {
    @Test
    fun reportsCapabilityAvailabilityFromInjectedProbe() {
        val available = setOf("wine", "box64", "vulkaninfo")
        val detector = ShellCapabilityDetector { cmd -> available.contains(cmd) }

        val report = detector.detect()

        assertEquals(true, report.wineAvailable)
        assertEquals(true, report.box64Available)
        assertEquals(false, report.fexAvailable)
        assertEquals(true, report.vulkanAvailable)
        assertTrue(report.diagnostics.isEmpty())
    }

    @Test
    fun emitsDiagnosticsWhenMissing() {
        val detector = ShellCapabilityDetector { false }

        val report = detector.detect()

        assertEquals(false, report.wineAvailable)
        assertEquals(false, report.box64Available)
        assertEquals(false, report.vulkanAvailable)
        assertTrue(report.diagnostics.any { it.contains("wine") })
        assertTrue(report.diagnostics.any { it.contains("box64") })
        assertTrue(report.diagnostics.any { it.contains("vulkaninfo") })
    }
}
