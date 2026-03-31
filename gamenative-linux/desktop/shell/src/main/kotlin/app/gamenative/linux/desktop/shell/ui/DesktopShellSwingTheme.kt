package app.gamenative.linux.desktop.shell.ui

import java.awt.Component
import java.awt.Container
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTabbedPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.border.Border

object DesktopShellSwingTheme {
    fun apply(frame: JFrame) {
        val tokens = DesktopShellThemeTokens.current
        frame.contentPane.background = tokens.colors.appBackground
        frame.rootPane.background = tokens.colors.appBackground
        val content = frame.contentPane
        if (content is JComponent) {
            applyRecursively(content)
        }
    }

    fun sectionBorder(title: String): Border {
        val tokens = DesktopShellThemeTokens.current
        return BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(tokens.colors.borderSubtle),
                title,
            ),
            BorderFactory.createEmptyBorder(
                tokens.spacing.sm,
                tokens.spacing.sm,
                tokens.spacing.sm,
                tokens.spacing.sm,
            ),
        )
    }

    private fun applyRecursively(component: Component) {
        style(component)
        if (component is Container) {
            component.components.forEach { child -> applyRecursively(child) }
        }
    }

    private fun style(component: Component) {
        val tokens = DesktopShellThemeTokens.current
        when (component) {
            is JPanel -> {
                component.background = tokens.colors.surfaceBackground
                component.foreground = tokens.colors.contentPrimary
            }

            is JTabbedPane -> {
                component.background = tokens.colors.surfaceBackground
                component.foreground = tokens.colors.contentPrimary
                component.font = Font(Font.SANS_SERIF, Font.BOLD, tokens.typography.headingSize)
            }

            is JLabel -> {
                component.foreground = tokens.colors.contentMuted
                component.font = Font(Font.SANS_SERIF, Font.PLAIN, tokens.typography.bodySize)
            }

            is JButton -> {
                component.background = tokens.colors.elevatedBackground
                component.foreground = tokens.colors.contentPrimary
                component.font = Font(Font.SANS_SERIF, Font.BOLD, tokens.typography.bodySize)
                component.isFocusPainted = true
            }

            is JTextField -> {
                component.background = tokens.colors.elevatedBackground
                component.foreground = tokens.colors.contentPrimary
                component.caretColor = tokens.colors.accent
                component.font = Font(Font.SANS_SERIF, Font.PLAIN, tokens.typography.bodySize)
                component.border = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(tokens.colors.borderSubtle),
                    BorderFactory.createEmptyBorder(
                        tokens.spacing.xs,
                        tokens.spacing.sm,
                        tokens.spacing.xs,
                        tokens.spacing.sm,
                    ),
                )
            }

            is JTextArea -> {
                component.background = tokens.colors.elevatedBackground
                component.foreground = tokens.colors.contentPrimary
                component.caretColor = tokens.colors.accent
                component.font = Font(Font.MONOSPACED, Font.PLAIN, tokens.typography.monoSize)
            }

            is JList<*> -> {
                component.background = tokens.colors.elevatedBackground
                component.foreground = tokens.colors.contentPrimary
                component.selectionBackground = tokens.colors.accent
                component.selectionForeground = tokens.colors.appBackground
                component.font = Font(Font.MONOSPACED, Font.PLAIN, tokens.typography.bodySize)
            }

            is JComboBox<*> -> {
                component.background = tokens.colors.elevatedBackground
                component.foreground = tokens.colors.contentPrimary
                component.font = Font(Font.SANS_SERIF, Font.PLAIN, tokens.typography.bodySize)
            }

            is JScrollPane -> {
                component.background = tokens.colors.surfaceBackground
                component.viewport.background = tokens.colors.elevatedBackground
                component.border = BorderFactory.createLineBorder(tokens.colors.borderSubtle)
            }
        }
    }
}
