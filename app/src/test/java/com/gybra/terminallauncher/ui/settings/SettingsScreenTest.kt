package com.gybra.terminallauncher.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import com.gybra.terminallauncher.shell.DosDrive
import com.gybra.terminallauncher.shell.PromptSymbol
import com.gybra.terminallauncher.shell.ShellType
import com.gybra.terminallauncher.theme.TerminalTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `renders and changes shell and clock controls`() {
        val harness = SettingsHarness()
        composeRule.setContent { harness.Content() }

        composeRule.onNodeWithText("(*) UNIX").assertIsDisplayed()
        composeRule.onNodeWithText("( ) DOS").performClick()
        composeRule.onNodeWithTag("settings-list").performScrollToNode(hasText("[*] Show clock"))
        composeRule.onNodeWithText("[*] Show clock").performClick()

        assertEquals(ShellType.DOS, harness.state.shellType)
        assertFalse(harness.state.showClock)
    }

    @Test
    fun `forwards every theme selection`() {
        val harness = SettingsHarness()
        composeRule.setContent { harness.Content() }

        val selectionOrder = listOf(
            TerminalTheme.GREEN,
            TerminalTheme.AMBER,
            TerminalTheme.MONOCHROME,
            TerminalTheme.SYSTEM,
        )
        selectionOrder.forEach { theme ->
            val optionText = "( ) ${theme.name}"
            composeRule.onNodeWithTag("settings-list").performScrollToNode(hasText(optionText))
            composeRule.onNodeWithText(optionText).performClick()

            assertEquals(theme, harness.state.terminalTheme)
        }
    }

    @Test
    fun `forwards username and hostname input`() {
        val harness = SettingsHarness()
        composeRule.setContent { harness.Content() }

        composeRule
            .onNodeWithTag("settings-list")
            .performScrollToNode(hasContentDescription("Username"))
        composeRule.onNodeWithContentDescription("Username").performTextReplacement("oreste")
        composeRule
            .onNodeWithTag("settings-list")
            .performScrollToNode(hasContentDescription("Hostname"))
        composeRule
            .onNodeWithContentDescription("Hostname")
            .performTextReplacement("phone")

        assertEquals("oreste", harness.state.username)
        assertEquals("phone", harness.state.hostname)
    }

    @Test
    fun `forwards every prompt symbol selection`() {
        val harness = SettingsHarness()
        composeRule.setContent { harness.Content() }

        listOf(PromptSymbol.PERCENT, PromptSymbol.ARROW, PromptSymbol.DOLLAR).forEach { symbol ->
            val optionText = "( ) ${symbol.text}"
            composeRule.onNodeWithTag("settings-list").performScrollToNode(hasText(optionText))
            composeRule.onNodeWithText(optionText).performClick()

            assertEquals(symbol, harness.state.promptSymbol)
        }
    }

    @Test
    fun `forwards every DOS drive selection`() {
        val harness = SettingsHarness()
        composeRule.setContent { harness.Content() }

        listOf(DosDrive.A, DosDrive.D, DosDrive.C).forEach { drive ->
            val optionText = "( ) ${drive.name}:"
            composeRule.onNodeWithTag("settings-list").performScrollToNode(hasText(optionText))
            composeRule.onNodeWithText(optionText).performClick()

            assertEquals(drive, harness.state.dosDrive)
        }
    }

    @Test
    fun `forwards the battery toggle`() {
        val harness = SettingsHarness()
        composeRule.setContent { harness.Content() }

        composeRule.onNodeWithTag("settings-list").performScrollToNode(hasText("[*] Show battery"))
        composeRule.onNodeWithText("[*] Show battery").performClick()

        assertFalse(harness.state.showBattery)
    }

    @Test
    fun `forwards the prompt path toggle`() {
        val harness = SettingsHarness()
        composeRule.setContent { harness.Content() }

        composeRule
            .onNodeWithTag("settings-list")
            .performScrollToNode(hasText("[*] Show path in prompt"))
        composeRule.onNodeWithText("[*] Show path in prompt").performClick()

        assertFalse(harness.state.showPromptPath)
    }

    @Test
    fun `renders unchecked clock and selected DOS state`() {
        val harness = SettingsHarness(
            initialState = defaultState().copy(
                shellType = ShellType.DOS,
                showClock = false,
                storageError = "Unable to save preferences",
            ),
        )
        composeRule.setContent { harness.Content() }

        composeRule.onNodeWithText("(*) DOS").assertIsDisplayed()
        composeRule.onNodeWithText("Unable to save preferences").assertIsDisplayed()
        composeRule.onNodeWithTag("settings-list").performScrollToNode(hasText("[ ] Show clock"))
        composeRule.onNodeWithText("[ ] Show clock").assertIsDisplayed()
    }

    @Test
    fun `forwards the explicit back action`() {
        val harness = SettingsHarness()
        composeRule.setContent { harness.Content() }

        composeRule.onNodeWithText("< back").performClick()

        assertTrue(harness.wentBack)
    }

    private fun defaultState(): SettingsUiState = SettingsUiState(
        shellType = ShellType.UNIX,
        terminalTheme = TerminalTheme.SYSTEM,
        showClock = true,
        showBattery = true,
        doubleTapToLock = false,
        username = "user",
        hostname = "android",
        promptSymbol = PromptSymbol.DOLLAR,
        showPromptPath = true,
        dosDrive = DosDrive.C,
    )

    private inner class SettingsHarness(
        initialState: SettingsUiState = defaultState(),
    ) {
        var state by mutableStateOf(initialState)
        var wentBack = false

        @Composable
        fun Content() {
            SettingsScreen(
                state = state,
                actions = SettingsActions(
                    selectShell = { state = state.copy(shellType = it) },
                    selectTheme = { state = state.copy(terminalTheme = it) },
                    setShowClock = { state = state.copy(showClock = it) },
                    setShowBattery = { state = state.copy(showBattery = it) },
                    setDoubleTapToLock = { state = state.copy(doubleTapToLock = it) },
                    setUsername = { state = state.copy(username = it) },
                    setHostname = { state = state.copy(hostname = it) },
                    selectPromptSymbol = { state = state.copy(promptSymbol = it) },
                    setShowPromptPath = { state = state.copy(showPromptPath = it) },
                    selectDosDrive = { state = state.copy(dosDrive = it) },
                ),
                onBack = { wentBack = true },
            )
        }
    }
}
