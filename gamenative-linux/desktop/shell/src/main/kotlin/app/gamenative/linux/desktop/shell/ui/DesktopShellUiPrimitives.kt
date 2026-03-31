package app.gamenative.linux.desktop.shell.ui

import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JTextField

object DesktopShellUiPrimitives {
    fun screenPanel(): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = BorderFactory.createEmptyBorder(
            DesktopShellThemeTokens.current.spacing.sm,
            DesktopShellThemeTokens.current.spacing.sm,
            DesktopShellThemeTokens.current.spacing.sm,
            DesktopShellThemeTokens.current.spacing.sm,
        )
        return panel
    }

    fun sectionPanel(title: String): JPanel {
        val panel = screenPanel()
        panel.border = DesktopShellSwingTheme.sectionBorder(title)
        return panel
    }

    fun fieldLabel(text: String): JLabel = JLabel(text)

    fun inputField(initialValue: String, columns: Int): JTextField {
        return JTextField(initialValue, columns)
    }

    fun outputArea(rows: Int, columns: Int): JTextArea {
        val area = JTextArea(rows, columns)
        area.isEditable = false
        area.lineWrap = true
        area.wrapStyleWord = true
        return area
    }

    fun actionButton(text: String, onClick: () -> Unit): JButton {
        val button = JButton(text)
        button.addActionListener { onClick() }
        return button
    }

    fun scroll(content: JComponent): JScrollPane {
        val pane = JScrollPane(content)
        val tokens = DesktopShellThemeTokens.current
        pane.preferredSize = Dimension(100, 100)
        pane.border = BorderFactory.createEmptyBorder(
            tokens.spacing.xs,
            0,
            tokens.spacing.xs,
            0,
        )
        return pane
    }
}
