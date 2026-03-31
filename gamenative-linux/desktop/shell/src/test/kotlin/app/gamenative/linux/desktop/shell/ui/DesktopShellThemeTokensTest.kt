package app.gamenative.linux.desktop.shell.ui

import kotlin.test.Test
import kotlin.test.assertTrue

class DesktopShellThemeTokensTest {
    @Test
    fun coreSpacingScaleIsMonotonic() {
        val spacing = DesktopShellThemeTokens.current.spacing
        assertTrue(spacing.xs > 0)
        assertTrue(spacing.sm > spacing.xs)
        assertTrue(spacing.md > spacing.sm)
        assertTrue(spacing.lg > spacing.md)
        assertTrue(spacing.xl > spacing.lg)
    }

    @Test
    fun typographyScaleIsValid() {
        val typography = DesktopShellThemeTokens.current.typography
        assertTrue(typography.headingSize >= typography.bodySize)
        assertTrue(typography.bodySize > 0)
        assertTrue(typography.monoSize > 0)
    }

    @Test
    fun colorRolesAreDistinctForReadability() {
        val colors = DesktopShellThemeTokens.current.colors
        assertTrue(colors.appBackground.rgb != colors.contentPrimary.rgb)
        assertTrue(colors.surfaceBackground.rgb != colors.contentPrimary.rgb)
        assertTrue(colors.accent.rgb != colors.appBackground.rgb)
    }
}
