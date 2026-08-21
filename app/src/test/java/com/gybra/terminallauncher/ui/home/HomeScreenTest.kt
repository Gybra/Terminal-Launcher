package com.gybra.terminallauncher.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.launcher.AppShortcut
import com.gybra.terminallauncher.search.SearchResult
import com.gybra.terminallauncher.search.SearchResult.Match
import com.gybra.terminallauncher.shell.LauncherLocation
import com.gybra.terminallauncher.shell.ShellContext
import com.gybra.terminallauncher.shell.dos.DosShellProfile
import com.gybra.terminallauncher.shell.unix.UnixShellProfile
import com.gybra.terminallauncher.ui.TestTag
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
                onLockScreen = {},
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
                onLockScreen = {},
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
                onLockScreen = {},
                promptActions = emptyPromptActions(),
            )
        }

        composeRule.onNodeWithText("Browser").assertDoesNotExist()
    }

    @Test
    fun `removes the status line when reactive state hides it`() {
        var state by mutableStateOf(homeState(statusClock = "22:10"))

        composeRule.setContent {
            HomeScreen(
                state = state,
                onAppClick = {},
                onShortcutClick = {},
                onSettingsClick = {},
                onLockScreen = {},
                promptActions = emptyPromptActions(),
            )
        }

        composeRule.onNodeWithText("22:10").assertIsDisplayed()
        state = state.copy(statusClock = null)
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
                onLockScreen = {},
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
                onLockScreen = {},
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
                onLockScreen = {},
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
                onLockScreen = {},
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
                onLockScreen = {},
                promptActions = emptyPromptActions(),
            )
        }

        composeRule.onNodeWithText("user@android:~$ ls").assertIsDisplayed()
        composeRule.onNodeWithText("camera").assertIsDisplayed()
        composeRule.onNodeWithText("telegram").assertIsDisplayed()
    }

    @Test
    fun `locks the screen on a double tap and leaves the rows alone`() {
        val app = InstalledApp(packageName = "com.example.camera", label = "Camera")
        val launched = mutableListOf<InstalledApp>()
        var locks = 0
        composeRule.setContent {
            HomeScreen(
                state = homeState().copy(apps = listOf(app)),
                onAppClick = { launchedApp -> launched += launchedApp },
                onShortcutClick = {},
                onSettingsClick = {},
                onLockScreen = { locks += 1 },
                promptActions = emptyPromptActions(),
            )
        }

        composeRule.onNodeWithText("camera").performClick()

        assertEquals(listOf(app), launched)
        assertEquals(0, locks)

        composeRule.onNodeWithTag(TestTag.HOME_LIST.tag).performTouchInput { doubleClick(bottomCenter) }
        composeRule.waitForIdle()

        assertEquals(1, locks)
        assertEquals(listOf(app), launched)
    }

    @Test
    fun `leaves a double tap the rows already handle alone`() {
        val app = InstalledApp(packageName = "com.example.camera", label = "Camera")
        val launched = mutableListOf<InstalledApp>()
        var locks = 0
        composeRule.setContent {
            HomeScreen(
                state = homeState().copy(apps = listOf(app)),
                onAppClick = { launchedApp -> launched += launchedApp },
                onShortcutClick = {},
                onSettingsClick = {},
                onLockScreen = { locks += 1 },
                promptActions = emptyPromptActions(),
            )
        }

        composeRule.onNodeWithText("camera").performTouchInput { doubleClick() }
        composeRule.waitForIdle()

        assertEquals(0, locks)
        assertEquals(listOf(app, app), launched)
    }

    @Test
    fun `keeps scrolling instead of locking on a swipe`() {
        var locks = 0
        composeRule.setContent {
            HomeScreen(
                state = homeState().copy(apps = manyApps()),
                onAppClick = {},
                onShortcutClick = {},
                onSettingsClick = {},
                onLockScreen = { locks += 1 },
                promptActions = emptyPromptActions(),
            )
        }

        composeRule.onNodeWithTag(TestTag.HOME_LIST.tag).performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        assertEquals(0, locks)
        composeRule.onNodeWithText("app 0").assertDoesNotExist()
    }

    @Test
    fun `keeps the clock on the left and the battery on the right of the status line`() {
        composeRule.setContent {
            HomeScreen(
                state = homeState(statusClock = "22:10", statusBattery = "42%"),
                onAppClick = {},
                onShortcutClick = {},
                onSettingsClick = {},
                onLockScreen = {},
                promptActions = emptyPromptActions(),
            )
        }

        val clock = composeRule.onNodeWithText("22:10").getUnclippedBoundsInRoot()
        val battery = composeRule.onNodeWithText("42%").getUnclippedBoundsInRoot()
        val screenWidth = composeRule.onRoot().getUnclippedBoundsInRoot().width

        assertTrue(clock.left < screenWidth / 2)
        assertTrue(battery.left > clock.right)
        assertTrue(battery.right > screenWidth / 2)
    }

    @Test
    fun `keeps the battery on the right when the clock is hidden`() {
        composeRule.setContent {
            HomeScreen(
                state = homeState(statusBattery = "42%"),
                onAppClick = {},
                onShortcutClick = {},
                onSettingsClick = {},
                onLockScreen = {},
                promptActions = emptyPromptActions(),
            )
        }

        val battery = composeRule.onNodeWithText("42%").getUnclippedBoundsInRoot()
        val screenWidth = composeRule.onRoot().getUnclippedBoundsInRoot().width

        assertTrue(battery.left > screenWidth / 2)
    }

    private fun manyApps(): List<InstalledApp> = List(30) { index ->
        InstalledApp(packageName = "com.example.app$index", label = "App $index")
    }

    private fun homeState(
        statusClock: String? = null,
        statusBattery: String? = null,
    ): HomeUiState = HomeUiState(
        shellProfile = UnixShellProfile,
        shellContext = defaultShellContext(),
        statusClock = statusClock,
        statusBattery = statusBattery,
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
        val shortcut = AppShortcut(
            packageName = "org.example.browser",
            id = "new-tab",
            label = "New Tab",
        )
        var startedShortcut: AppShortcut? = null
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
                onLockScreen = {},
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
