package com.gybra.terminallauncher.ui

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.launcher.SystemScreen
import com.gybra.terminallauncher.shell.LauncherLocation
import com.gybra.terminallauncher.shell.ShellContext
import com.gybra.terminallauncher.shell.ShellProfile
import com.gybra.terminallauncher.shell.DosDrive
import com.gybra.terminallauncher.shell.PromptSymbol
import com.gybra.terminallauncher.shell.ShellType
import com.gybra.terminallauncher.shell.dos.DosShellProfile
import com.gybra.terminallauncher.shell.unix.UnixShellProfile
import com.gybra.terminallauncher.theme.TerminalTheme
import com.gybra.terminallauncher.theme.colors
import com.gybra.terminallauncher.ui.home.HomeUiState
import com.gybra.terminallauncher.ui.home.PromptActions
import com.gybra.terminallauncher.ui.home.SubmittedAction
import com.gybra.terminallauncher.ui.settings.SettingsActions
import com.gybra.terminallauncher.ui.settings.SettingsEntry
import com.gybra.terminallauncher.ui.settings.SettingsUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
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
class LauncherAppTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `opens settings and returns home with the back action`() {
        composeRule.setContent {
            LauncherApp(
                homeState = homeState(),
                settingsState = settingsState(),
                settingsActions = emptySettingsActions(),
                promptActions = emptyPromptActions(),
                submittedActions = flowOf(SubmittedAction.OpenSettings),
                onLaunchApp = {},
                onLaunchShortcut = {},
                onLockScreen = {},
                onOpenSystemScreen = {},
                onRestartLauncher = {},
            )
        }

        composeRule.onNodeWithText(SettingsEntry.APPEARANCE.label).assertIsDisplayed()

        composeRule.onNodeWithText(SettingsEntry.BACK.label).performClick()
        composeRule.onNodeWithText("user@android:~$", substring = true).assertIsDisplayed()
    }

    @Test
    fun `forwards a double tap on Home to the screen lock`() {
        var locks = 0
        composeRule.setContent {
            LauncherApp(
                homeState = homeState(
                    apps = listOf(InstalledApp(packageName = "com.example.mail", label = "Mail")),
                ),
                settingsState = settingsState(),
                settingsActions = emptySettingsActions(),
                promptActions = emptyPromptActions(),
                submittedActions = emptyFlow(),
                onLaunchApp = {},
                onLaunchShortcut = {},
                onLockScreen = { locks += 1 },
                onOpenSystemScreen = {},
                onRestartLauncher = {},
            )
        }

        composeRule.onNodeWithTag(TestTag.HOME_LIST.tag).performTouchInput { doubleClick(topCenter) }
        composeRule.waitForIdle()

        assertEquals(1, locks)
    }

    @Test
    fun `system back returns from settings to home`() {
        var backDispatcher: OnBackPressedDispatcher? = null
        composeRule.setContent {
            backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
            LauncherApp(
                homeState = homeState(),
                settingsState = settingsState(),
                settingsActions = emptySettingsActions(),
                promptActions = emptyPromptActions(),
                submittedActions = flowOf(SubmittedAction.OpenSettings),
                onLaunchApp = {},
                onLaunchShortcut = {},
                onLockScreen = {},
                onOpenSystemScreen = {},
                onRestartLauncher = {},
            )
        }
        composeRule.onNodeWithText(SettingsEntry.APPEARANCE.label).assertIsDisplayed()

        composeRule.runOnIdle {
            checkNotNull(backDispatcher).onBackPressed()
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("user@android:~$", substring = true).assertIsDisplayed()
    }

    @Test
    fun `opens settings when a submitted line asks for it`() {
        val submittedActions = MutableSharedFlow<SubmittedAction>(extraBufferCapacity = 1)
        composeRule.setContent {
            LauncherApp(
                homeState = homeState(),
                settingsState = settingsState(),
                settingsActions = emptySettingsActions(),
                promptActions = emptyPromptActions(),
                submittedActions = submittedActions,
                onLaunchApp = {},
                onLaunchShortcut = {},
                onLockScreen = {},
                onOpenSystemScreen = {},
                onRestartLauncher = {},
            )
        }

        composeRule.runOnIdle { submittedActions.tryEmit(SubmittedAction.OpenSettings) }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(SettingsEntry.APPEARANCE.label).assertIsDisplayed()
    }

    @Test
    fun `opens the system screen a submitted line asked for`() {
        val submittedActions = MutableSharedFlow<SubmittedAction>(extraBufferCapacity = 1)
        var openedScreen: SystemScreen? = null
        composeRule.setContent {
            LauncherApp(
                homeState = homeState(),
                settingsState = settingsState(),
                settingsActions = emptySettingsActions(),
                promptActions = emptyPromptActions(),
                submittedActions = submittedActions,
                onLaunchApp = {},
                onLaunchShortcut = {},
                onLockScreen = {},
                onOpenSystemScreen = { screen -> openedScreen = screen },
                onRestartLauncher = {},
            )
        }

        composeRule.runOnIdle {
            submittedActions.tryEmit(SubmittedAction.OpenSystemScreen(SystemScreen.WifiSettings))
        }
        composeRule.waitForIdle()

        assertEquals(SystemScreen.WifiSettings, openedScreen)
    }

    @Test
    fun `restarts the launcher when a submitted line asks for it`() {
        val submittedActions = MutableSharedFlow<SubmittedAction>(extraBufferCapacity = 1)
        var restarts = 0
        composeRule.setContent {
            LauncherApp(
                homeState = homeState(),
                settingsState = settingsState(),
                settingsActions = emptySettingsActions(),
                promptActions = emptyPromptActions(),
                submittedActions = submittedActions,
                onLaunchApp = {},
                onLaunchShortcut = {},
                onLockScreen = {},
                onOpenSystemScreen = {},
                onRestartLauncher = { restarts += 1 },
            )
        }

        composeRule.runOnIdle { submittedActions.tryEmit(SubmittedAction.RestartLauncher) }
        composeRule.waitForIdle()

        assertEquals(1, restarts)
    }

    @Test
    fun `launches the application a submitted line resolved`() {
        val app = InstalledApp(packageName = "org.example.mail", label = "Mail")
        val submittedActions = MutableSharedFlow<SubmittedAction>(extraBufferCapacity = 1)
        var launchedApp: InstalledApp? = null
        composeRule.setContent {
            LauncherApp(
                homeState = homeState(),
                settingsState = settingsState(),
                settingsActions = emptySettingsActions(),
                promptActions = emptyPromptActions(),
                submittedActions = submittedActions,
                onLaunchApp = { launched -> launchedApp = launched },
                onLaunchShortcut = {},
                onLockScreen = {},
                onOpenSystemScreen = {},
                onRestartLauncher = {},
            )
        }

        composeRule.runOnIdle { submittedActions.tryEmit(SubmittedAction.LaunchApp(app)) }
        composeRule.waitForIdle()

        assertEquals(app, launchedApp)
    }

    @Test
    fun `theme changes immediately recolor Home content`() {
        var settingsState by mutableStateOf(settingsState(TerminalTheme.GREEN))
        composeRule.setContent {
            LauncherApp(
                homeState = homeState(),
                settingsState = settingsState,
                settingsActions = emptySettingsActions(),
                promptActions = emptyPromptActions(),
                submittedActions = emptyFlow(),
                onLaunchApp = {},
                onLaunchShortcut = {},
                onLockScreen = {},
                onOpenSystemScreen = {},
                onRestartLauncher = {},
            )
        }

        val greenColors = TerminalTheme.GREEN.colors(systemDarkTheme = false)
        assertBackgroundColor(greenColors.background)
        assertRenderedColor(greenColors.foreground)

        composeRule.runOnIdle {
            settingsState = settingsState.copy(terminalTheme = TerminalTheme.SYSTEM)
        }

        val systemColors = TerminalTheme.SYSTEM.colors(systemDarkTheme = false)
        assertBackgroundColor(systemColors.background)
        assertRenderedColor(systemColors.foreground)
    }

    @Test
    fun `every theme renders Home with both shell profiles`() {
        val app = InstalledApp(packageName = "org.example.mail", label = "Mail")
        var homeState by mutableStateOf(homeState())
        var settingsState by mutableStateOf(settingsState())
        composeRule.setContent {
            LauncherApp(
                homeState = homeState,
                settingsState = settingsState,
                settingsActions = emptySettingsActions(),
                promptActions = emptyPromptActions(),
                submittedActions = emptyFlow(),
                onLaunchApp = {},
                onLaunchShortcut = {},
                onLockScreen = {},
                onOpenSystemScreen = {},
                onRestartLauncher = {},
            )
        }

        val shells = listOf(
            ShellCase(
                type = ShellType.UNIX,
                profile = UnixShellProfile,
                appName = "mail",
                prompt = "user@android:~$",
            ),
            ShellCase(
                type = ShellType.DOS,
                profile = DosShellProfile,
                appName = "MAIL.EXE",
                prompt = "C:\\HOME>",
            ),
        )
        shells.forEach { shell ->
            TerminalTheme.entries.forEach { theme ->
                composeRule.runOnIdle {
                    homeState = homeState(
                        shellProfile = shell.profile,
                        apps = listOf(app),
                    )
                    settingsState = settingsState(
                        terminalTheme = theme,
                        shellType = shell.type,
                    )
                }

                composeRule.onNodeWithText(shell.appName).assertIsDisplayed()
                composeRule.onNodeWithText(shell.prompt, substring = true).assertIsDisplayed()
                val colors = theme.colors(systemDarkTheme = false)
                assertBackgroundColor(colors.background)
                assertRenderedColor(colors.foreground)
            }
        }
    }

    private fun assertBackgroundColor(color: Color) {
        val pixels = composeRule.onRoot().captureToImage().toPixelMap()
        val actual = pixels[pixels.width - 1, pixels.height - 1]
        assertTrue("Expected $color but rendered $actual", actual == color)
    }

    private fun assertRenderedColor(color: Color) {
        val pixels = composeRule.onRoot().captureToImage().toPixelMap()
        val colorFound = (0 until pixels.width).any { x ->
            (0 until pixels.height).any { y -> pixels[x, y] == color }
        }
        assertTrue("Expected rendered content to contain $color", colorFound)
    }

    private fun homeState(
        shellProfile: ShellProfile = UnixShellProfile,
        apps: List<InstalledApp> = emptyList(),
    ): HomeUiState = HomeUiState(
        shellProfile = shellProfile,
        shellContext = ShellContext(
            username = "user",
            hostname = "android",
            location = LauncherLocation.HOME,
        ),
        apps = apps,
    )

    private fun settingsState(
        terminalTheme: TerminalTheme = TerminalTheme.SYSTEM,
        shellType: ShellType = ShellType.UNIX,
    ): SettingsUiState = SettingsUiState(
        shellType = shellType,
        terminalTheme = terminalTheme,
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

    private fun emptySettingsActions(): SettingsActions = SettingsActions(
        selectShell = {},
        selectTheme = {},
        setShowClock = {},
        setShowBattery = {},
        setImmersiveMode = {},
        setDoubleTapToLock = {},
        setUsername = {},
        setHostname = {},
        selectPromptSymbol = {},
        setShowPromptPath = {},
        selectDosDrive = {},
    )

    private fun emptyPromptActions(): PromptActions = PromptActions(
        updateValue = {},
        updateFocus = {},
        submit = {},
    )

    private data class ShellCase(
        val type: ShellType,
        val profile: ShellProfile,
        val appName: String,
        val prompt: String,
    )
}
