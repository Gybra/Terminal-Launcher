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
    fun `renders current settings and forwards every user change`() {
        var selectedShell: ShellType? = null
        var showClock = true
        var username = ""
        var hostname = ""
        var wentBack = false
        var state by mutableStateOf(defaultState())

        composeRule.setContent {
            SettingsScreen(
                state = state,
                onShellSelected = {
                    selectedShell = it
                    state = state.copy(shellType = it)
                },
                onShowClockChanged = {
                    showClock = it
                    state = state.copy(showClock = it)
                },
                onUsernameChanged = {
                    username = it
                    state = state.copy(username = it)
                },
                onHostnameChanged = {
                    hostname = it
                    state = state.copy(hostname = it)
                },
                onBack = { wentBack = true },
            )
        }

        composeRule.onNodeWithText("(*) UNIX").assertIsDisplayed()
        composeRule.onNodeWithText("( ) DOS").performClick()
        composeRule.onNodeWithText("[*] Show clock").performClick()
        composeRule.onNodeWithText("< back").performClick()
        composeRule.onNodeWithContentDescription("Username").performTextReplacement("oreste")
        composeRule
            .onNodeWithTag("settings-list")
            .performScrollToNode(hasContentDescription("Hostname"))
        composeRule
            .onNodeWithContentDescription("Hostname")
            .performTextReplacement("phone")

        assertEquals(ShellType.DOS, selectedShell)
        assertFalse(showClock)
        assertEquals("oreste", username)
        assertEquals("phone", hostname)
        assertTrue(wentBack)
    }

    @Test
    fun `renders unchecked clock and selected DOS state`() {
        composeRule.setContent {
            SettingsScreen(
                state = defaultState().copy(shellType = ShellType.DOS, showClock = false),
                onShellSelected = {},
                onShowClockChanged = {},
                onUsernameChanged = {},
                onHostnameChanged = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithText("(*) DOS").assertIsDisplayed()
        composeRule.onNodeWithText("[ ] Show clock").assertIsDisplayed()
    }

    private fun defaultState(): SettingsUiState = SettingsUiState(
        shellType = ShellType.UNIX,
        showClock = true,
        username = "user",
        hostname = "android",
    )
}
