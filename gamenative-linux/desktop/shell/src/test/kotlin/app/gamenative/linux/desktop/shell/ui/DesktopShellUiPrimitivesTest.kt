package app.gamenative.linux.desktop.shell.ui

import javax.swing.BoxLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopShellUiPrimitivesTest {
    @Test
    fun outputAreaDefaultsToReadOnlyWrappedText() {
        val area = DesktopShellUiPrimitives.outputArea(rows = 4, columns = 10)

        assertFalse(area.isEditable)
        assertTrue(area.lineWrap)
        assertTrue(area.wrapStyleWord)
        assertEquals(4, area.rows)
        assertEquals(10, area.columns)
    }

    @Test
    fun sectionPanelUsesVerticalLayout() {
        val panel = DesktopShellUiPrimitives.sectionPanel("Sample")

        val layout = panel.layout as BoxLayout
        assertEquals(BoxLayout.Y_AXIS, layout.axis)
        assertTrue(panel.border != null)
    }

    @Test
    fun actionButtonExecutesHandler() {
        var clicked = false
        val button = DesktopShellUiPrimitives.actionButton("Run") {
            clicked = true
        }

        button.doClick()

        assertTrue(clicked)
        assertEquals("Run", button.text)
    }
}
