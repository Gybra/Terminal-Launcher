package com.gybra.terminallauncher.ui.pin

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.gybra.terminallauncher.launcher.PinnedShortcut
import com.gybra.terminallauncher.shell.dos.DosShellProfile
import com.gybra.terminallauncher.shell.unix.UnixShellProfile
import com.gybra.terminallauncher.theme.TerminalTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PinShortcutScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val shortcut = PinnedShortcut(
        packageName = "org.example.browser",
        id = "new-tab",
        label = "New Tab",
    )

    @Test
    fun `names the shortcut and the application that asks for it`() {
        composeRule.setContent {
            PinShortcutScreen(state = unixState(), onAccept = {}, onDecline = {})
        }

        composeRule.onNodeWithText("org.example.browser asks to pin").assertIsDisplayed()
        composeRule.onNodeWithText("new tab").assertIsDisplayed()
    }

    @Test
    fun `writes the confirmation in the style of the running shell`() {
        composeRule.setContent {
            PinShortcutScreen(
                state = unixState().copy(shellProfile = DosShellProfile),
                onAccept = {},
                onDecline = {},
            )
        }

        composeRule.onNodeWithText("ORG.EXAMPLE.BROWSER ASKS TO PIN").assertIsDisplayed()
        composeRule.onNodeWithText("NEW TAB.LNK").assertIsDisplayed()
        composeRule.onNodeWithText("[ PIN ]").assertIsDisplayed()
        composeRule.onNodeWithText("[ CANCEL ]").assertIsDisplayed()
    }

    @Test
    fun `answers the request the user chose`() {
        val answers = mutableListOf<String>()
        composeRule.setContent {
            PinShortcutScreen(
                state = unixState(),
                onAccept = { answers += "accept" },
                onDecline = { answers += "decline" },
            )
        }

        composeRule.onNodeWithText("[ pin ]").performClick()
        composeRule.onNodeWithText("[ cancel ]").performClick()

        assertEquals(listOf("accept", "decline"), answers)
    }

    private fun unixState(): PinShortcutUiState = PinShortcutUiState(
        shortcut = shortcut,
        shellProfile = UnixShellProfile,
        terminalTheme = TerminalTheme.SYSTEM,
    )
}
