package com.gybra.terminallauncher.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

public data class TerminalColors(
    public val background: Color,
    public val foreground: Color,
    public val secondary: Color,
)

public fun TerminalTheme.colors(systemDarkTheme: Boolean): TerminalColors = when (this) {
    TerminalTheme.SYSTEM -> systemColors(systemDarkTheme)
    TerminalTheme.GREEN -> TerminalColors(
        background = Color.Black,
        foreground = Color(0xFF00FF66),
        secondary = Color(0xFF00AA44),
    )
    TerminalTheme.AMBER -> TerminalColors(
        background = Color.Black,
        foreground = Color(0xFFFFB000),
        secondary = Color(0xFFB87900),
    )
    TerminalTheme.MONOCHROME -> darkMonochromeColors()
}

public fun TerminalColors.useDarkSystemBarIcons(): Boolean =
    background.luminance() > LIGHT_BACKGROUND_LUMINANCE

private fun systemColors(systemDarkTheme: Boolean): TerminalColors = if (systemDarkTheme) {
    darkMonochromeColors()
} else {
    TerminalColors(
        background = Color.White,
        foreground = Color.Black,
        secondary = Color(0xFF555555),
    )
}

private fun darkMonochromeColors(): TerminalColors = TerminalColors(
    background = Color.Black,
    foreground = Color.White,
    secondary = Color(0xFFAAAAAA),
)

private const val LIGHT_BACKGROUND_LUMINANCE = 0.5f
