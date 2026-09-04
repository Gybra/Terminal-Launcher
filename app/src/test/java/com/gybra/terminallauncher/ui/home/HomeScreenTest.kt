package com.gybra.terminallauncher.ui.home

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.down
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.launcher.AppShortcut
import com.gybra.terminallauncher.search.SearchResult
import com.gybra.terminallauncher.search.SearchResult.Match
import com.gybra.terminallauncher.shell.LauncherLocation
import com.gybra.terminallauncher.shell.ShellContext
import com.gybra.terminallauncher.shell.dos.DosShellProfile
import com.gybra.terminallauncher.shell.unix.UnixShellProfile
import com.gybra.terminallauncher.theme.TerminalTheme
import com.gybra.terminallauncher.theme.colors
import com.gybra.terminallauncher.ui.TestTag
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "notnight")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
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
    fun `announces the results with what the shell counted`() {
        val app = InstalledApp(packageName = "com.example.mail", label = "Mail")
        composeRule.setContent {
            HomeScreen(
                state = homeState().copy(
                    searchResults = listOf(SearchResult(app = app, match = Match.EXACT)),
                    prompt = PromptState(input = "mai"),
                ),
                onAppClick = {},
                onShortcutClick = {},
                onLockScreen = {},
                promptActions = emptyPromptActions(),
            )
        }

        composeRule.onNodeWithText("1 match:").assertIsDisplayed().assertHasNoClickAction()
    }

    @Test
    fun `says so when the typed line matches nothing`() {
        composeRule.setContent {
            HomeScreen(
                state = homeState().copy(prompt = PromptState(input = "zzz")),
                onAppClick = {},
                onShortcutClick = {},
                onLockScreen = {},
                promptActions = emptyPromptActions(),
            )
        }

        composeRule.onNodeWithText("no matches").assertIsDisplayed()
    }

    @Test
    fun `announces nothing while the prompt is empty`() {
        composeRule.setContent {
            HomeScreen(
                state = homeState(),
                onAppClick = {},
                onShortcutClick = {},
                onLockScreen = {},
                promptActions = emptyPromptActions(),
            )
        }

        composeRule.onNodeWithText("no matches").assertDoesNotExist()
    }

    @Test
    fun `closes the DOS results with what it counted, and says when it found none`() {
        val app = InstalledApp(packageName = "com.example.mail", label = "Mail")
        var state by mutableStateOf(
            homeState().copy(
                shellProfile = DosShellProfile,
                searchResults = listOf(SearchResult(app = app, match = Match.EXACT)),
                prompt = PromptState(input = "MAI"),
            ),
        )
        composeRule.setContent {
            HomeScreen(
                state = state,
                onAppClick = {},
                onShortcutClick = {},
                onLockScreen = {},
                promptActions = emptyPromptActions(),
            )
        }
        composeRule.onNodeWithText("1 File(s) found").assertIsDisplayed()

        composeRule.runOnIdle {
            state = state.copy(searchResults = emptyList(), prompt = PromptState(input = "ZZZ"))
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("File not found").assertIsDisplayed()
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
                onLockScreen = { locks += 1 },
                promptActions = emptyPromptActions(),
            )
        }

        composeRule.onNodeWithText("camera").performClick()

        assertEquals(listOf(app), launched)
        assertEquals(0, locks)

        composeRule.onNodeWithTag(TestTag.HOME_LIST.tag).performTouchInput { doubleClick(topCenter) }
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
        var notifications = 0
        composeRule.setContent {
            HomeScreen(
                state = homeState().copy(apps = manyApps()),
                onAppClick = {},
                onShortcutClick = {},
                onLockScreen = { locks += 1 },
                onExpandNotifications = { notifications += 1 },
                promptActions = emptyPromptActions(),
            )
        }

        composeRule.onNodeWithTag(TestTag.HOME_LIST.tag).performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        assertEquals(0, locks)
        assertEquals(0, notifications)
        composeRule.onNodeWithText("app 0").assertDoesNotExist()
    }

    @Test
    fun `opens notifications on a swipe down from the left of Home`() {
        var notifications = 0
        var quickSettings = 0
        composeRule.setContent {
            HomeScreen(
                state = homeState(),
                onAppClick = {},
                onShortcutClick = {},
                onLockScreen = {},
                onExpandNotifications = { notifications += 1 },
                onExpandQuickSettings = { quickSettings += 1 },
                promptActions = emptyPromptActions(),
            )
        }

        composeRule.onNodeWithTag(TestTag.HOME_LIST.tag).performTouchInput {
            swipe(
                start = percentOffset(x = 0.25f, y = 0.8f),
                end = percentOffset(x = 0.25f, y = 0.95f),
            )
        }
        composeRule.waitForIdle()

        assertEquals(1, notifications)
        assertEquals(0, quickSettings)
    }

    @Test
    fun `opens quick settings on a swipe down from the right of Home`() {
        var notifications = 0
        var quickSettings = 0
        composeRule.setContent {
            HomeScreen(
                state = homeState(),
                onAppClick = {},
                onShortcutClick = {},
                onLockScreen = {},
                onExpandNotifications = { notifications += 1 },
                onExpandQuickSettings = { quickSettings += 1 },
                promptActions = emptyPromptActions(),
            )
        }

        composeRule.onNodeWithTag(TestTag.HOME_LIST.tag).performTouchInput {
            swipe(
                start = percentOffset(x = 0.75f, y = 0.8f),
                end = percentOffset(x = 0.75f, y = 0.95f),
            )
        }
        composeRule.waitForIdle()

        assertEquals(0, notifications)
        assertEquals(1, quickSettings)
    }

    @Test
    fun `keeps scrolling instead of opening a shade on a swipe from the upper half`() {
        var notifications = 0
        var quickSettings = 0
        var overview = 0
        composeRule.setContent {
            HomeScreen(
                state = homeState().copy(apps = manyApps()),
                onAppClick = {},
                onShortcutClick = {},
                onLockScreen = {},
                onExpandNotifications = { notifications += 1 },
                onExpandQuickSettings = { quickSettings += 1 },
                onOpenOverview = { overview += 1 },
                promptActions = emptyPromptActions(),
            )
        }

        composeRule.onNodeWithTag(TestTag.HOME_LIST.tag).performTouchInput {
            swipe(
                start = percentOffset(x = 0.25f, y = 0.1f),
                end = percentOffset(x = 0.25f, y = 0.4f),
            )
        }
        composeRule.waitForIdle()

        assertEquals(0, notifications)
        assertEquals(0, quickSettings)
        assertEquals(0, overview)
    }

    @Test
    fun `opens Overview on a swipe up from the lower half of Home`() {
        var overview = 0
        var notifications = 0
        composeRule.setContent {
            HomeScreen(
                state = homeState().copy(
                    apps = listOf(InstalledApp(packageName = "com.example.camera", label = "Camera")),
                ),
                onAppClick = {},
                onShortcutClick = {},
                onLockScreen = {},
                onExpandNotifications = { notifications += 1 },
                onOpenOverview = { overview += 1 },
                promptActions = emptyPromptActions(),
            )
        }

        composeRule.onNodeWithTag(TestTag.HOME_LIST.tag).performTouchInput {
            swipe(
                start = percentOffset(x = 0.5f, y = 0.85f),
                end = percentOffset(x = 0.5f, y = 0.15f),
            )
        }
        composeRule.waitForIdle()

        assertEquals(1, overview)
        assertEquals(0, notifications)
    }

    @Test
    fun `leaves a swipe up from the upper half to scroll`() {
        var overview = 0
        composeRule.setContent {
            HomeScreen(
                state = homeState().copy(apps = manyApps()),
                onAppClick = {},
                onShortcutClick = {},
                onLockScreen = {},
                onOpenOverview = { overview += 1 },
                promptActions = emptyPromptActions(),
            )
        }

        composeRule.onNodeWithTag(TestTag.HOME_LIST.tag).performTouchInput {
            swipe(
                start = percentOffset(x = 0.5f, y = 0.15f),
                end = percentOffset(x = 0.5f, y = 0.05f),
            )
        }
        composeRule.waitForIdle()

        assertEquals(0, overview)
    }

    @Test
    fun `keeps the clock on the left and the battery on the right of the status line`() {
        composeRule.setContent {
            HomeScreen(
                state = homeState(statusClock = "22:10", statusBattery = "42%"),
                onAppClick = {},
                onShortcutClick = {},
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
        writeShortcutCommand = {},
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

    @Test
    fun `offers the commands a held application can write without starting it`() {
        val app = InstalledApp(packageName = "com.example.camera", label = "Camera")
        var offered: Pair<InstalledApp, String>? = null
        var launched: InstalledApp? = null
        composeRule.setContent {
            HomeScreen(
                state = HomeUiState(
                    shellProfile = UnixShellProfile,
                    shellContext = defaultShellContext(),
                    apps = listOf(app),
                ),
                onAppClick = { launched = it },
                onShortcutClick = {},
                onLockScreen = {},
                promptActions = emptyPromptActions().copy(
                    offerAppCommands = { held, rowKey -> offered = held to rowKey },
                ),
            )
        }

        composeRule.onNodeWithText("camera").performTouchInput { longClick() }

        assertEquals(app to app.packageName, offered)
        assertNull(launched)
    }

    @Test
    fun `writes the chosen command and moves to the prompt`() {
        val app = InstalledApp(packageName = "com.example.camera", label = "Camera")
        val pin = HoldChoice(label = "pin", line = "pin Camera")
        var written: HoldChoice? = null
        composeRule.setContent {
            HomeScreen(
                state = HomeUiState(
                    shellProfile = UnixShellProfile,
                    shellContext = defaultShellContext(),
                    apps = listOf(app),
                    holdChoices = listOf(
                        pin,
                        HoldChoice(label = "uninstall", line = "uninstall Camera"),
                    ),
                    holdRowKey = app.packageName,
                ),
                onAppClick = {},
                onShortcutClick = {},
                onLockScreen = {},
                promptActions = emptyPromptActions().copy(writeChoice = { written = it }),
            )
        }

        composeRule.onNodeWithText("pin").assertHeightIsAtLeast(48.dp).performClick()

        assertEquals(pin, written)
        composeRule.onNodeWithTag(TestTag.PROMPT_INPUT.tag).assertIsFocused()
    }

    @Test
    fun `sits hold choices under the held application in the arrested colour`() {
        val camera = InstalledApp(packageName = "com.example.camera", label = "Camera")
        val mail = InstalledApp(packageName = "com.example.mail", label = "Mail")
        composeRule.setContent {
            HomeScreen(
                state = HomeUiState(
                    shellProfile = UnixShellProfile,
                    shellContext = defaultShellContext(),
                    apps = listOf(camera, mail),
                    holdChoices = listOf(
                        HoldChoice(label = "pin", line = "pin Camera"),
                        HoldChoice(label = "uninstall", line = "uninstall Camera"),
                    ),
                    holdRowKey = camera.packageName,
                ),
                onAppClick = {},
                onShortcutClick = {},
                onLockScreen = {},
                promptActions = emptyPromptActions(),
            )
        }

        val cameraRow = composeRule.onNodeWithText("camera").getUnclippedBoundsInRoot()
        val pin = composeRule.onNodeWithText("pin").getUnclippedBoundsInRoot()
        val uninstall = composeRule.onNodeWithText("uninstall").getUnclippedBoundsInRoot()
        val mailRow = composeRule.onNodeWithText("mail").getUnclippedBoundsInRoot()

        assertEquals(cameraRow.bottom.value, pin.top.value, 0.5f)
        assertEquals(pin.bottom.value, uninstall.top.value, 0.5f)
        assertEquals(uninstall.bottom.value, mailRow.top.value, 0.5f)

        val choice = pixelsOf("pin")
        assertTrue("Expected the arrested colour", choice.contains(terminalColors().secondary))
        assertTrue("Expected no full colour", !choice.contains(terminalColors().foreground))
    }

    @Test
    fun `dismisses unanswered hold choices on system back`() {
        var dismissed = false
        var backDispatcher: OnBackPressedDispatcher? = null
        composeRule.setContent {
            backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
            HomeScreen(
                state = HomeUiState(
                    shellProfile = UnixShellProfile,
                    shellContext = defaultShellContext(),
                    holdChoices = listOf(HoldChoice(label = "pin", line = "pin Camera")),
                ),
                onAppClick = {},
                onShortcutClick = {},
                onLockScreen = {},
                promptActions = emptyPromptActions().copy(dismissChoices = { dismissed = true }),
            )
        }

        composeRule.runOnIdle { checkNotNull(backDispatcher).onBackPressed() }

        assertEquals(true, dismissed)
    }

    @Test
    fun `writes the command a held shortcut offers`() {
        val shortcut = AppShortcut(
            packageName = "com.example.browser",
            id = "new-tab",
            label = "New Tab",
        )
        var written: AppShortcut? = null
        composeRule.setContent {
            HomeScreen(
                state = HomeUiState(
                    shellProfile = UnixShellProfile,
                    shellContext = defaultShellContext(),
                    shortcuts = listOf(shortcut),
                ),
                onAppClick = {},
                onShortcutClick = {},
                onLockScreen = {},
                promptActions = emptyPromptActions().copy(writeShortcutCommand = { written = it }),
            )
        }

        composeRule.onNodeWithText("new tab").performTouchInput { longClick() }

        assertEquals(shortcut, written)
    }

    @Test
    fun `names the pinned rows with the inert section the shell writes`() {
        composeRule.setContent {
            HomeScreen(
                state = homeState().copy(
                    apps = listOf(InstalledApp(packageName = "com.example.mail", label = "Mail")),
                    shortcuts = listOf(
                        AppShortcut(packageName = "com.example.mail", id = "inbox", label = "Inbox"),
                    ),
                ),
                onAppClick = {},
                onShortcutClick = {},
                onLockScreen = {},
                promptActions = emptyPromptActions(),
            )
        }

        composeRule.onNodeWithText("~/pinned:").assertIsDisplayed().assertHasNoClickAction()
        val section = pixelsOf("~/pinned:")
        assertTrue("Expected the arrested colour", section.contains(terminalColors().secondary))
        assertTrue("Expected no full colour", !section.contains(terminalColors().foreground))
    }

    @Test
    fun `closes the DOS pinned section with what it counted`() {
        composeRule.setContent {
            HomeScreen(
                state = homeState().copy(
                    shellProfile = DosShellProfile,
                    apps = listOf(
                        InstalledApp(packageName = "com.example.mail", label = "Mail"),
                        InstalledApp(packageName = "com.example.maps", label = "Maps"),
                    ),
                ),
                onAppClick = {},
                onShortcutClick = {},
                onLockScreen = {},
                promptActions = emptyPromptActions(),
            )
        }

        composeRule.onNodeWithText("Directory of C:\\HOME\\PINNED").assertIsDisplayed()
        composeRule.onNodeWithText("2 File(s)").assertIsDisplayed()
    }

    @Test
    fun `writes no pinned section when nothing is pinned`() {
        composeRule.setContent {
            HomeScreen(
                state = homeState(),
                onAppClick = {},
                onShortcutClick = {},
                onLockScreen = {},
                promptActions = emptyPromptActions(),
            )
        }

        composeRule.onNodeWithText("~/pinned:").assertDoesNotExist()
    }

    @Test
    fun `writes the empty Home line the shell wrote, above the prompt`() {
        composeRule.setContent {
            HomeScreen(
                state = HomeUiState(
                    shellProfile = DosShellProfile,
                    shellContext = defaultShellContext(),
                    helpInvitation = "TYPE HELP TO LIST THE COMMANDS",
                ),
                onAppClick = {},
                onShortcutClick = {},
                onLockScreen = {},
                promptActions = emptyPromptActions(),
            )
        }

        composeRule.onNodeWithText("TYPE HELP TO LIST THE COMMANDS").assertIsDisplayed()
    }

    @Test
    fun `starts an application listed in the terminal history when it is tapped`() {
        val app = InstalledApp(packageName = "com.example.mailbox", label = "Mailbox")
        var launched: InstalledApp? = null
        composeRule.setContent {
            HomeScreen(
                state = HomeUiState(
                    shellProfile = UnixShellProfile,
                    shellContext = defaultShellContext(),
                    history = listOf(
                        TerminalEntry(
                            id = 0L,
                            input = "mailb",
                            output = listOf("mailb matches more than one application"),
                            apps = listOf(app),
                        ),
                    ),
                ),
                onAppClick = { launched = it },
                onShortcutClick = {},
                onLockScreen = {},
                promptActions = emptyPromptActions(),
            )
        }

        composeRule.onNodeWithText("mailb matches more than one application").assertIsDisplayed()
        composeRule
            .onNodeWithText("mailbox")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertEquals(app, launched)
    }

    @Test
    fun `starts a shortcut listed in the terminal history when it is tapped`() {
        val shortcut = AppShortcut(
            packageName = "org.example.browser",
            id = "new-tab",
            label = "New Tab",
        )
        var startedShortcut: AppShortcut? = null
        composeRule.setContent {
            HomeScreen(
                state = HomeUiState(
                    shellProfile = DosShellProfile,
                    shellContext = defaultShellContext(),
                    history = listOf(
                        TerminalEntry(
                            id = 0L,
                            input = "SHORTCUTS BROWSER",
                            output = emptyList(),
                            shortcuts = listOf(shortcut),
                        ),
                    ),
                ),
                onAppClick = {},
                onShortcutClick = { startedShortcut = it },
                onLockScreen = {},
                promptActions = emptyPromptActions(),
            )
        }

        composeRule
            .onNodeWithText("NEW TAB.LNK")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertEquals(shortcut, startedShortcut)
    }

    @Test
    fun `keeps the prompt anchored while the rows scroll`() {
        composeRule.setContent {
            HomeScreen(
                state = homeState().copy(apps = manyApps()),
                onAppClick = {},
                onShortcutClick = {},
                onLockScreen = {},
                promptActions = emptyPromptActions(),
            )
        }

        val anchored = composeRule.onNodeWithTag(TestTag.PROMPT_INPUT.tag).getUnclippedBoundsInRoot()
        composeRule.onNodeWithTag(TestTag.HOME_LIST.tag).performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TestTag.PROMPT_INPUT.tag).assertIsDisplayed()
        assertEquals(
            anchored,
            composeRule.onNodeWithTag(TestTag.PROMPT_INPUT.tag).getUnclippedBoundsInRoot(),
        )
    }

    @Test
    fun `keeps the status line visible while the rows scroll`() {
        composeRule.setContent {
            HomeScreen(
                state = homeState(statusClock = "22:10").copy(apps = manyApps()),
                onAppClick = {},
                onShortcutClick = {},
                onLockScreen = {},
                promptActions = emptyPromptActions(),
            )
        }

        composeRule.onNodeWithTag(TestTag.HOME_LIST.tag).performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("22:10").assertIsDisplayed()
    }

    @Test
    fun `scrolls the newest printed line into view`() {
        var state by mutableStateOf(homeState().copy(apps = manyApps()))
        composeRule.setContent {
            HomeScreen(
                state = state,
                onAppClick = {},
                onShortcutClick = {},
                onLockScreen = {},
                promptActions = emptyPromptActions(),
            )
        }

        state = state.copy(
            history = listOf(TerminalEntry(id = 0L, input = "battery", output = listOf("87%"))),
        )
        composeRule.waitForIdle()

        composeRule.onNodeWithText("87%").assertIsDisplayed()
    }

    @Test
    fun `keeps no settings row on Home`() {
        composeRule.setContent {
            HomeScreen(
                state = homeState(),
                onAppClick = {},
                onShortcutClick = {},
                onLockScreen = {},
                promptActions = emptyPromptActions(),
            )
        }

        composeRule.onNodeWithText("settings").assertDoesNotExist()
    }

    @Test
    fun `centres the text of a startable row in its tappable height`() {
        val app = InstalledApp(packageName = "com.example.mail", label = "Mail")
        composeRule.setContent {
            HomeScreen(
                state = homeState().copy(apps = listOf(app)),
                onAppClick = {},
                onShortcutClick = {},
                onLockScreen = {},
                promptActions = emptyPromptActions(),
            )
        }

        val row = composeRule.onNodeWithText("mail").getUnclippedBoundsInRoot()
        val text = composeRule
            .onNodeWithText("mail", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()

        assertTrue("Expected the text inside the row, got $text in $row", text.height < row.height)
        assertEquals((text.top - row.top).value, (row.bottom - text.bottom).value, 1f)
    }

    @Test
    fun `leaves no gap between startable rows`() {
        val apps = listOf(
            InstalledApp(packageName = "com.example.mail", label = "Mail"),
            InstalledApp(packageName = "com.example.camera", label = "Camera"),
        )
        composeRule.setContent {
            HomeScreen(
                state = homeState().copy(apps = apps),
                onAppClick = {},
                onShortcutClick = {},
                onLockScreen = {},
                promptActions = emptyPromptActions(),
            )
        }

        val mail = composeRule.onNodeWithText("mail").getUnclippedBoundsInRoot()
        val camera = composeRule.onNodeWithText("camera").getUnclippedBoundsInRoot()

        assertEquals(mail.bottom.value, camera.top.value, 0.5f)
    }

    @Test
    fun `writes command output in the arrested colour`() {
        composeRule.setContent {
            HomeScreen(
                state = homeState().copy(
                    history = listOf(TerminalEntry(id = 0L, input = "ls", output = listOf("camera"))),
                ),
                onAppClick = {},
                onShortcutClick = {},
                onLockScreen = {},
                promptActions = emptyPromptActions(),
            )
        }

        val output = pixelsOf("camera")

        assertTrue("Expected the arrested colour", output.contains(terminalColors().secondary))
        assertTrue("Expected no full colour", !output.contains(terminalColors().foreground))
    }

    @Test
    fun `writes a startable row in the full colour`() {
        val app = InstalledApp(packageName = "com.example.mail", label = "Mail")
        composeRule.setContent {
            HomeScreen(
                state = homeState().copy(apps = listOf(app)),
                onAppClick = {},
                onShortcutClick = {},
                onLockScreen = {},
                promptActions = emptyPromptActions(),
            )
        }

        val row = pixelsOf("mail")

        assertTrue("Expected the full colour", row.contains(terminalColors().foreground))
        assertTrue("Expected no arrested colour", !row.contains(terminalColors().secondary))
    }

    @Test
    fun `inverts a startable row while it is pressed`() {
        val app = InstalledApp(packageName = "com.example.mail", label = "Mail")
        composeRule.setContent {
            HomeScreen(
                state = homeState().copy(apps = listOf(app)),
                onAppClick = {},
                onShortcutClick = {},
                onLockScreen = {},
                promptActions = emptyPromptActions(),
            )
        }

        composeRule.onNodeWithText("mail").performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(PRESS_DELAY_MILLIS)
        composeRule.waitForIdle()

        val pressed = pixelsOf("mail")

        assertEquals(terminalColors().foreground, pressed.mostPainted())
        assertTrue("Expected the text cut out", pressed.contains(terminalColors().background))

        composeRule.onNodeWithText("mail").performTouchInput { up() }
        composeRule.waitForIdle()

        val released = pixelsOf("mail")

        assertEquals(terminalColors().background, released.mostPainted())
        assertTrue("Expected the text back", released.contains(terminalColors().foreground))
    }

    private fun pixelsOf(text: String): List<Color> {
        val pixels = composeRule.onNodeWithText(text).captureToImage().toPixelMap()
        return (0 until pixels.width)
            .flatMap { x -> (0 until pixels.height).map { y -> pixels[x, y] } }
    }

    /** The colour a row is mostly painted in, which is what an inversion swaps. */
    private fun List<Color>.mostPainted(): Color =
        groupingBy { colour -> colour }.eachCount().maxBy { painted -> painted.value }.key

    private fun terminalColors() = TerminalTheme.MONOCHROME.colors(systemDarkTheme = true)
}

private const val PRESS_DELAY_MILLIS = 300L
