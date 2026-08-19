package com.gybra.terminallauncher.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
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
import com.gybra.terminallauncher.shell.ShellType
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
        composeRule.onNodeWithText("[*] Show clock").performClick()

        assertEquals(ShellType.DOS, harness.state.shellType)
        assertFalse(harness.state.showClock)
    }

    @Test
    fun `forwards username and hostname input`() {
        val harness = SettingsHarness()
        composeRule.setContent { harness.Content() }

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
        composeRule.onNodeWithText("[ ] Show clock").assertIsDisplayed()
        composeRule.onNodeWithText("Unable to save preferences").assertIsDisplayed()
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
        showClock = true,
        username = "user",
        hostname = "android",
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
                onShellSelected = { state = state.copy(shellType = it) },
                onShowClockChanged = { state = state.copy(showClock = it) },
                onUsernameChanged = { state = state.copy(username = it) },
                onHostnameChanged = { state = state.copy(hostname = it) },
                onBack = { wentBack = true },
            )
        }
    }
}
