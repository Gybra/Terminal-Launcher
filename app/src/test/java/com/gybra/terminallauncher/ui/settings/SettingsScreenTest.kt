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
import com.gybra.terminallauncher.shell.ShellProfile
import com.gybra.terminallauncher.shell.ShellType
import com.gybra.terminallauncher.shell.ShellProfiles
import com.gybra.terminallauncher.shell.dos.DosShellProfile
import com.gybra.terminallauncher.shell.unix.UnixShellProfile
import com.gybra.terminallauncher.theme.TerminalTheme
import com.gybra.terminallauncher.ui.TestTag
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

        composeRule
            .onNodeWithText(optionText(ShellType.UNIX.name, selected = true))
            .assertIsDisplayed()
        composeRule.onNodeWithText(optionText(ShellType.DOS.name)).performClick()

        val clock = toggleText(SettingsEntry.SHOW_CLOCK, checked = true, profile = DosShellProfile)
        composeRule.onNodeWithTag(TestTag.SETTINGS_LIST.tag).performScrollToNode(hasText(clock))
        composeRule.onNodeWithText(clock).performClick()

        assertEquals(DosShellProfile, harness.state.shellProfile)
        assertFalse(harness.state.showClock)
    }

    @Test
    fun `reads every label in the shell that was chosen`() {
        val harness = SettingsHarness()
        composeRule.setContent { harness.Content() }

        composeRule.onNodeWithText(label(SettingsEntry.BACK)).assertIsDisplayed()
        composeRule.onNodeWithText(label(SettingsEntry.APPEARANCE)).assertIsDisplayed()

        composeRule.onNodeWithText(optionText(ShellType.DOS.name)).performClick()

        composeRule
            .onNodeWithText(label(SettingsEntry.BACK, profile = DosShellProfile))
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(label(SettingsEntry.APPEARANCE, profile = DosShellProfile))
            .assertIsDisplayed()
    }

    @Test
    fun `forwards every theme selection`() {
        val harness = SettingsHarness()
        composeRule.setContent { harness.Content() }

        // The selected theme is written as `(*)`, so it is clicked last, once something else holds it.
        val selectionOrder = TerminalTheme.entries.filterNot { theme ->
            theme == harness.state.terminalTheme
        } + harness.state.terminalTheme
        selectionOrder.forEach { theme ->
            val option = optionText(theme.name)
            composeRule.onNodeWithTag(TestTag.SETTINGS_LIST.tag).performScrollToNode(hasText(option))
            composeRule.onNodeWithText(option).performClick()

            assertEquals(theme, harness.state.terminalTheme)
        }
    }

    @Test
    fun `forwards username and hostname input`() {
        val harness = SettingsHarness()
        composeRule.setContent { harness.Content() }

        composeRule
            .onNodeWithTag(TestTag.SETTINGS_LIST.tag)
            .performScrollToNode(hasContentDescription(label(SettingsEntry.USERNAME)))
        composeRule.onNodeWithContentDescription(label(SettingsEntry.USERNAME)).performTextReplacement("oreste")
        composeRule
            .onNodeWithTag(TestTag.SETTINGS_LIST.tag)
            .performScrollToNode(hasContentDescription(label(SettingsEntry.HOSTNAME)))
        composeRule
            .onNodeWithContentDescription(label(SettingsEntry.HOSTNAME))
            .performTextReplacement("phone")

        assertEquals("oreste", harness.state.username)
        assertEquals("phone", harness.state.hostname)
    }

    @Test
    fun `forwards every prompt symbol selection`() {
        val harness = SettingsHarness()
        composeRule.setContent { harness.Content() }

        listOf(PromptSymbol.PERCENT, PromptSymbol.ARROW, PromptSymbol.DOLLAR).forEach { symbol ->
            val option = optionText(symbol.text)
            composeRule.onNodeWithTag(TestTag.SETTINGS_LIST.tag).performScrollToNode(hasText(option))
            composeRule.onNodeWithText(option).performClick()

            assertEquals(symbol, harness.state.promptSymbol)
        }
    }

    @Test
    fun `forwards every DOS drive selection`() {
        val harness = SettingsHarness()
        composeRule.setContent { harness.Content() }

        listOf(DosDrive.A, DosDrive.D, DosDrive.C).forEach { drive ->
            val option = optionText("${drive.name}:")
            composeRule.onNodeWithTag(TestTag.SETTINGS_LIST.tag).performScrollToNode(hasText(option))
            composeRule.onNodeWithText(option).performClick()

            assertEquals(drive, harness.state.dosDrive)
        }
    }

    @Test
    fun `forwards the battery toggle`() {
        val harness = SettingsHarness()
        composeRule.setContent { harness.Content() }

        composeRule.onNodeWithTag(TestTag.SETTINGS_LIST.tag).performScrollToNode(hasText(toggleText(SettingsEntry.SHOW_BATTERY, checked = true)))
        composeRule.onNodeWithText(toggleText(SettingsEntry.SHOW_BATTERY, checked = true)).performClick()

        assertFalse(harness.state.showBattery)
    }

    @Test
    fun `forwards the immersive mode toggle`() {
        val harness = SettingsHarness()
        composeRule.setContent { harness.Content() }

        composeRule.onNodeWithTag(TestTag.SETTINGS_LIST.tag).performScrollToNode(hasText(toggleText(SettingsEntry.IMMERSIVE_MODE, checked = true)))
        composeRule.onNodeWithText(toggleText(SettingsEntry.IMMERSIVE_MODE, checked = true)).performClick()

        assertFalse(harness.state.immersiveMode)
    }

    @Test
    fun `forwards the prompt path toggle`() {
        val harness = SettingsHarness()
        composeRule.setContent { harness.Content() }

        composeRule
            .onNodeWithTag(TestTag.SETTINGS_LIST.tag)
            .performScrollToNode(hasText(toggleText(SettingsEntry.SHOW_PROMPT_PATH, checked = true)))
        composeRule.onNodeWithText(toggleText(SettingsEntry.SHOW_PROMPT_PATH, checked = true)).performClick()

        assertFalse(harness.state.showPromptPath)
    }

    @Test
    fun `renders unchecked clock and selected DOS state`() {
        val harness = SettingsHarness(
            initialState = defaultState().copy(
                shellProfile = DosShellProfile,
                showClock = false,
                storageError = "Unable to save preferences",
            ),
        )
        composeRule.setContent { harness.Content() }

        val dosClock = toggleText(SettingsEntry.SHOW_CLOCK, checked = false, profile = DosShellProfile)
        composeRule
            .onNodeWithText(optionText(ShellType.DOS.name, selected = true, profile = DosShellProfile))
            .assertIsDisplayed()
        composeRule.onNodeWithText("UNABLE TO SAVE PREFERENCES").assertIsDisplayed()
        composeRule.onNodeWithTag(TestTag.SETTINGS_LIST.tag).performScrollToNode(hasText(dosClock))
        composeRule.onNodeWithText(dosClock).assertIsDisplayed()
    }

    @Test
    fun `forwards the explicit back action`() {
        val harness = SettingsHarness()
        composeRule.setContent { harness.Content() }

        composeRule.onNodeWithText(label(SettingsEntry.BACK)).performClick()

        assertTrue(harness.wentBack)
    }

    private fun toggleText(
        entry: SettingsEntry,
        checked: Boolean,
        profile: ShellProfile = UnixShellProfile,
    ): String = "[${if (checked) "*" else " "}] ${profile.formatMessage(entry.label)}"

    private fun optionText(
        label: String,
        selected: Boolean = false,
        profile: ShellProfile = UnixShellProfile,
    ): String = "${if (selected) "(*)" else "( )"} ${profile.formatMessage(label)}"

    /** The label the screen writes for [entry], which the selected shell decides. */
    private fun label(
        entry: SettingsEntry,
        profile: ShellProfile = UnixShellProfile,
    ): String = profile.formatMessage(entry.label)

    private fun defaultState(): SettingsUiState = SettingsUiState(
        shellProfile = UnixShellProfile,
        terminalTheme = TerminalTheme.SYSTEM,
        showClock = true,
        showBattery = true,
        immersiveMode = true,
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
                    selectShell = { state = state.copy(shellProfile = ShellProfiles.forType(it)) },
                    selectTheme = { state = state.copy(terminalTheme = it) },
                    setShowClock = { state = state.copy(showClock = it) },
                    setShowBattery = { state = state.copy(showBattery = it) },
                    setImmersiveMode = { state = state.copy(immersiveMode = it) },
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
