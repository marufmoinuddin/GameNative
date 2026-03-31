package app.gamenative.linux.cli

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CliMainTest {
    @Test
    fun usageTextMentionsRealOnlyMode() {
        val usage = usageText()

        assertTrue(usage.contains("REAL JavaSteam network mode is always enabled."))
        assertFalse(usage.contains("--mode"))
    }

    @Test
    fun helpFlagDetectionSupportsShortAndLongForms() {
        assertTrue(hasHelpFlag(arrayOf("--help")))
        assertTrue(hasHelpFlag(arrayOf("-h")))
        assertFalse(hasHelpFlag(arrayOf("--verbose")))
    }
}
