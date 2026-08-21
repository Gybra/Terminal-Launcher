package com.gybra.terminallauncher.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalThemeTest {
    @Test
    fun `system theme follows the platform brightness`() {
        val darkColors = TerminalTheme.SYSTEM.colors(systemDarkTheme = true)
        val lightColors = TerminalTheme.SYSTEM.colors(systemDarkTheme = false)

        assertEquals(Color.Black, darkColors.background)
        assertEquals(Color.White, darkColors.foreground)
        assertEquals(Color.White, lightColors.background)
        assertEquals(Color.Black, lightColors.foreground)
    }

    @Test
    fun `fixed themes ignore the platform brightness`() {
        fixedThemes().forEach { theme ->
            assertEquals(
                theme.colors(systemDarkTheme = true),
                theme.colors(systemDarkTheme = false),
            )
        }
    }

    @Test
    fun `C64 and Solarized carry palettes of their own`() {
        val c64 = TerminalTheme.C64.colors(systemDarkTheme = true)
        val solarized = TerminalTheme.SOLARIZED.colors(systemDarkTheme = true)

        assertEquals(Color(0xFF40318D), c64.background)
        assertEquals(Color(0xFF7869C4), c64.foreground)
        assertEquals(Color(0xFF5A4BA8), c64.secondary)
        assertEquals(Color(0xFF002B36), solarized.background)
        assertEquals(Color(0xFF93A1A1), solarized.foreground)
        assertEquals(Color(0xFF586E75), solarized.secondary)
    }

    @Test
    fun `every theme writes its inert color between the background and the foreground`() {
        listOf(true, false).forEach { systemDarkTheme ->
            TerminalTheme.entries.forEach { theme ->
                val colors = theme.colors(systemDarkTheme)
                val background = colors.background.luminance()
                val foreground = colors.foreground.luminance()
                val inert = colors.secondary.luminance()

                assertTrue(
                    "$theme writes output at $inert, outside $background and $foreground",
                    inert > minOf(background, foreground) && inert < maxOf(background, foreground),
                )
            }
        }
    }

    @Test
    fun `fixed color themes have distinct foregrounds`() {
        val foregrounds = TerminalTheme.entries.map { theme ->
            theme.colors(systemDarkTheme = true).foreground
        }

        assertEquals(TerminalTheme.entries.size - 1, foregrounds.toSet().size)
        assertNotEquals(
            TerminalTheme.GREEN.colors(systemDarkTheme = true).secondary,
            TerminalTheme.AMBER.colors(systemDarkTheme = true).secondary,
        )
    }

    @Test
    fun `system bar icons contrast with light and dark backgrounds`() {
        fixedThemes().forEach { theme ->
            assertEquals(false, theme.colors(systemDarkTheme = true).useDarkSystemBarIcons())
        }

        assertEquals(
            true,
            TerminalTheme.SYSTEM.colors(systemDarkTheme = false).useDarkSystemBarIcons(),
        )
    }

    /** Every theme but the one that follows the platform, which brings its own brightness. */
    private fun fixedThemes(): List<TerminalTheme> =
        TerminalTheme.entries.filterNot { theme -> theme == TerminalTheme.SYSTEM }
}
