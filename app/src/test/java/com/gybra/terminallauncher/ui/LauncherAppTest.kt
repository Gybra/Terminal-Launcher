package com.gybra.terminallauncher.ui

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.gybra.terminallauncher.shell.LauncherLocation
import com.gybra.terminallauncher.shell.ShellContext
import com.gybra.terminallauncher.shell.ShellType
import com.gybra.terminallauncher.shell.unix.UnixShellProfile
import com.gybra.terminallauncher.ui.home.HomeUiState
import com.gybra.terminallauncher.ui.settings.SettingsActions
import com.gybra.terminallauncher.ui.settings.SettingsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
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
                onAppClick = {},
            )
        }

        composeRule.onNodeWithText("settings").performClick()
        composeRule.onNodeWithText("Appearance").assertIsDisplayed()

        composeRule.onNodeWithText("< back").performClick()
        composeRule.onNodeWithText("user@android:~$ _").assertIsDisplayed()
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
                onAppClick = {},
            )
        }
        composeRule.onNodeWithText("settings").performClick()

        composeRule.runOnIdle {
            checkNotNull(backDispatcher).onBackPressed()
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("user@android:~$ _").assertIsDisplayed()
    }

    private fun homeState(): HomeUiState = HomeUiState(
        shellProfile = UnixShellProfile,
        shellContext = ShellContext(
            username = "user",
            hostname = "android",
            location = LauncherLocation.HOME,
        ),
    )

    private fun settingsState(): SettingsUiState = SettingsUiState(
        shellType = ShellType.UNIX,
        showClock = true,
        username = "user",
        hostname = "android",
    )

    private fun emptySettingsActions(): SettingsActions = SettingsActions(
        selectShell = {},
        setShowClock = {},
        setUsername = {},
        setHostname = {},
    )
}
