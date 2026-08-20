package com.gybra.terminallauncher.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.launcher.PinnedShortcut
import com.gybra.terminallauncher.search.SearchResult
import com.gybra.terminallauncher.search.SearchResult.Match
import com.gybra.terminallauncher.shell.LauncherLocation
import com.gybra.terminallauncher.shell.ShellContext
import com.gybra.terminallauncher.shell.dos.DosShellProfile
import com.gybra.terminallauncher.shell.unix.UnixShellProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `renders application labels and forwards clicks`() {
        val app = InstalledApp(packageName = "com.example.browser", label = "Browser")
        var clickedApp: InstalledApp? = null

        composeRule.setContent {
            HomeScreen(
                state = HomeUiState(
                    shellProfile = UnixShellProfile,
                    shellContext = defaultShellContext(),
                    apps = listOf(app),
                ),
                onAppClick = { clickedApp = it },
                onShortcutClick = {},
                onSettingsClick = {},
                promptActions = emptyPromptActions(),
            )
        }

        composeRule
            .onNodeWithText("browser")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertEquals(app, clickedApp)
    }

    @Test
    fun `delegates application naming to the selected shell profile`() {
        val app = InstalledApp(packageName = "com.example.browser", label = "Browser")

        composeRule.setContent {
            HomeScreen(
                state = HomeUiState(
                    apps = listOf(app),
                    shellProfile = DosShellProfile,
                    shellContext = defaultShellContext(),
                ),
                onAppClick = {},
                onShortcutClick = {},
                onSettingsClick = {},
                promptActions = emptyPromptActions(),
            )
        }

        composeRule.onNodeWithText("BROWSER.EXE").assertIsDisplayed()
        composeRule.onNodeWithText("Browser").assertDoesNotExist()
    }

    @Test
    fun `renders no application rows for empty state`() {
        composeRule.setContent {
            HomeScreen(
                state = HomeUiState(
                    shellProfile = UnixShellProfile,
                    shellContext = defaultShellContext(),
                ),
                onAppClick = {},
                onShortcutClick = {},
                onSettingsClick = {},
                promptActions = emptyPromptActions(),
            )
        }

        composeRule.onNodeWithText("Browser").assertDoesNotExist()
    }

    @Test
    fun `removes the status line when reactive state hides it`() {
        var state by mutableStateOf(homeState(statusText = "22:10"))

        composeRule.setContent {
            HomeScreen(
                state = state,
                onAppClick = {},
                onShortcutClick = {},
                onSettingsClick = {},
                promptActions = emptyPromptActions(),
            )
        }

        composeRule.onNodeWithText("22:10").assertIsDisplayed()
        state = state.copy(statusText = null)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("22:10").assertDoesNotExist()
    }

    @Test
    fun `reacts to prompt identity and shell profile changes`() {
        var state by mutableStateOf(homeState())
        composeRule.setContent {
            HomeScreen(
                state = state,
                onAppClick = {},
                onShortcutClick = {},
                onSettingsClick = {},
                promptActions = emptyPromptActions(),
            )
        }

        state = state.copy(
            shellContext = ShellContext("oreste", "phone", LauncherLocation.HOME),
        )
        composeRule.onNodeWithText("oreste@phone:~$", substring = true).assertIsDisplayed()

        state = state.copy(shellProfile = DosShellProfile)
        composeRule.onNodeWithText("C:\\HOME>", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("oreste@phone:~$", substring = true).assertDoesNotExist()
    }

    @Test
    fun `forwards settings clicks`() {
        var settingsClicked = false
        composeRule.setContent {
            HomeScreen(
                state = homeState(),
                onAppClick = {},
                onShortcutClick = {},
                onSettingsClick = { settingsClicked = true },
                promptActions = emptyPromptActions(),
            )
        }

        composeRule.onNodeWithText("settings").performClick()

        assertTrue(settingsClicked)
    }

    @Test
    fun `renders shell-formatted search results and forwards clicks`() {
        val app = InstalledApp(packageName = "com.example.mail", label = "Mail")
        var clickedApp: InstalledApp? = null

        composeRule.setContent {
            HomeScreen(
                state = HomeUiState(
                    shellProfile = DosShellProfile,
                    shellContext = defaultShellContext(),
                    searchResults = listOf(SearchResult(app = app, match = Match.EXACT)),
                ),
                onAppClick = { clickedApp = it },
                onShortcutClick = {},
                onSettingsClick = {},
                promptActions = emptyPromptActions(),
            )
        }

        composeRule
            .onNodeWithText("MAIL.EXE")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertEquals(app, clickedApp)
    }

    @Test
    fun `renders a matching pinned application as both a pinned row and a result`() {
        val app = InstalledApp(packageName = "com.example.mail", label = "Mail")

        composeRule.setContent {
            HomeScreen(
                state = HomeUiState(
                    shellProfile = UnixShellProfile,
                    shellContext = defaultShellContext(),
                    apps = listOf(app),
                    searchResults = listOf(SearchResult(app = app, match = Match.EXACT)),
                ),
                onAppClick = {},
                onShortcutClick = {},
                onSettingsClick = {},
                promptActions = emptyPromptActions(),
            )
        }

        composeRule.onAllNodesWithText("mail").assertCountEquals(2)
    }

    @Test
    fun `renders the terminal history above the prompt`() {
        composeRule.setContent {
            HomeScreen(
                state = HomeUiState(
                    shellProfile = UnixShellProfile,
                    shellContext = defaultShellContext(),
                    history = listOf(
                        TerminalEntry(id = 0L, input = "ls", output = listOf("camera", "telegram")),
                    ),
                ),
                onAppClick = {},
                onShortcutClick = {},
                onSettingsClick = {},
                promptActions = emptyPromptActions(),
            )
        }

        composeRule.onNodeWithText("user@android:~$ ls").assertIsDisplayed()
        composeRule.onNodeWithText("camera").assertIsDisplayed()
        composeRule.onNodeWithText("telegram").assertIsDisplayed()
    }

    private fun homeState(statusText: String? = null): HomeUiState = HomeUiState(
        shellProfile = UnixShellProfile,
        shellContext = defaultShellContext(),
        statusText = statusText,
    )

    private fun defaultShellContext(): ShellContext = ShellContext(
        username = "user",
        hostname = "android",
        location = LauncherLocation.HOME,
    )

    private fun emptyPromptActions(): PromptActions = PromptActions(
        updateValue = {},
        updateFocus = {},
        submit = {},
    )

    @Test
    fun `lists the shortcuts pinned to Home and starts the one that is tapped`() {
        val shortcut = PinnedShortcut(
            packageName = "org.example.browser",
            id = "new-tab",
            label = "New Tab",
        )
        var startedShortcut: PinnedShortcut? = null
        composeRule.setContent {
            HomeScreen(
                state = HomeUiState(
                    shellProfile = UnixShellProfile,
                    shellContext = defaultShellContext(),
                    shortcuts = listOf(shortcut),
                ),
                onAppClick = {},
                onShortcutClick = { startedShortcut = it },
                onSettingsClick = {},
                promptActions = emptyPromptActions(),
            )
        }

        composeRule
            .onNodeWithText("new tab")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertEquals(shortcut, startedShortcut)
    }
}
