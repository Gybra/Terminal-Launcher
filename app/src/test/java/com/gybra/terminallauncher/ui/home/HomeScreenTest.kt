package com.gybra.terminallauncher.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.gybra.terminallauncher.launcher.InstalledApp
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
                onSettingsClick = {},
                promptActions = emptyPromptActions(),
            )
        }

        composeRule.onNodeWithText("Browser").assertDoesNotExist()
    }

    @Test
    fun `removes the clock when reactive state hides it`() {
        var state by mutableStateOf(homeState(clockText = "22:10"))

        composeRule.setContent {
            HomeScreen(
                state = state,
                onAppClick = {},
                onSettingsClick = {},
                promptActions = emptyPromptActions(),
            )
        }

        composeRule.onNodeWithText("22:10").assertIsDisplayed()
        state = state.copy(clockText = null)
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
                onSettingsClick = { settingsClicked = true },
                promptActions = emptyPromptActions(),
            )
        }

        composeRule.onNodeWithText("settings").performClick()

        assertTrue(settingsClicked)
    }

    private fun homeState(clockText: String? = null): HomeUiState = HomeUiState(
        shellProfile = UnixShellProfile,
        shellContext = defaultShellContext(),
        clockText = clockText,
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
}
