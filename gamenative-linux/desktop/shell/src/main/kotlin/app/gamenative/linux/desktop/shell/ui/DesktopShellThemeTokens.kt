package app.gamenative.linux.desktop.shell.ui

import java.awt.Color

data class DesktopSpacingTokens(
    val xs: Int,
    val sm: Int,
    val md: Int,
    val lg: Int,
    val xl: Int,
)

data class DesktopTypographyTokens(
    val headingSize: Int,
    val bodySize: Int,
    val monoSize: Int,
)

data class DesktopColorTokens(
    val appBackground: Color,
    val surfaceBackground: Color,
    val elevatedBackground: Color,
    val contentPrimary: Color,
    val contentMuted: Color,
    val accent: Color,
    val borderSubtle: Color,
)

data class DesktopShapeTokens(
    val surfaceRadius: Int,
    val controlRadius: Int,
)

data class DesktopShellThemeTokenBundle(
    val spacing: DesktopSpacingTokens,
    val typography: DesktopTypographyTokens,
    val colors: DesktopColorTokens,
    val shapes: DesktopShapeTokens,
)

object DesktopShellThemeTokens {
    val current = DesktopShellThemeTokenBundle(
        spacing = DesktopSpacingTokens(
            xs = 4,
            sm = 8,
            md = 12,
            lg = 16,
            xl = 24,
        ),
        typography = DesktopTypographyTokens(
            headingSize = 15,
            bodySize = 13,
            monoSize = 12,
        ),
        colors = DesktopColorTokens(
            appBackground = Color(0x0E, 0x13, 0x1E),
            surfaceBackground = Color(0x15, 0x1C, 0x2B),
            elevatedBackground = Color(0x1A, 0x23, 0x35),
            contentPrimary = Color(0xE9, 0xEE, 0xF8),
            contentMuted = Color(0xB5, 0xC0, 0xD8),
            accent = Color(0x62, 0x9E, 0xFF),
            borderSubtle = Color(0x2B, 0x37, 0x4F),
        ),
        shapes = DesktopShapeTokens(
            surfaceRadius = 12,
            controlRadius = 8,
        ),
    )
}
