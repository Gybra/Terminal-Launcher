package com.gybra.terminallauncher.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
        listOf(TerminalTheme.GREEN, TerminalTheme.AMBER, TerminalTheme.MONOCHROME).forEach { theme ->
            assertEquals(
                theme.colors(systemDarkTheme = true),
                theme.colors(systemDarkTheme = false),
            )
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
        val darkColors = TerminalTheme.MONOCHROME.colors(systemDarkTheme = true)
        val lightColors = TerminalTheme.SYSTEM.colors(systemDarkTheme = false)

        assertEquals(false, darkColors.useDarkSystemBarIcons())
        assertEquals(true, lightColors.useDarkSystemBarIcons())
    }
}
