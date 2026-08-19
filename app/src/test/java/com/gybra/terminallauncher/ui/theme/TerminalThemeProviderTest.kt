package com.gybra.terminallauncher.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.gybra.terminallauncher.theme.TerminalColors
import com.gybra.terminallauncher.theme.TerminalTheme
import com.gybra.terminallauncher.theme.colors
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TerminalThemeProviderTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `updates provided colors when the selected theme changes`() {
        var theme by mutableStateOf(TerminalTheme.GREEN)
        var observedColors: TerminalColors? = null
        composeRule.setContent {
            TerminalThemeProvider(theme = theme) {
                observedColors = LocalTerminalColors.current
            }
        }

        assertEquals(TerminalTheme.GREEN.colors(systemDarkTheme = true), observedColors)

        composeRule.runOnIdle { theme = TerminalTheme.AMBER }
        composeRule.waitForIdle()

        assertEquals(TerminalTheme.AMBER.colors(systemDarkTheme = true), observedColors)
    }
}
