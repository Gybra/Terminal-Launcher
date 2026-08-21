package com.gybra.terminallauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.launcher.AppShortcut
import com.gybra.terminallauncher.launcher.SystemScreen
import com.gybra.terminallauncher.ui.home.HomeScreen
import com.gybra.terminallauncher.ui.home.HomeUiState
import com.gybra.terminallauncher.ui.home.PromptActions
import com.gybra.terminallauncher.ui.home.SubmittedAction
import com.gybra.terminallauncher.ui.settings.SettingsActions
import com.gybra.terminallauncher.ui.settings.SettingsScreen
import com.gybra.terminallauncher.ui.settings.SettingsUiState
import com.gybra.terminallauncher.ui.theme.TerminalThemeProvider
import kotlinx.coroutines.flow.Flow

/**
 * Shows Home or the settings, and turns what a row or a submitted line asks into the launcher
 * work the composition root wired. Starting anything from a row calls [onRowStart] first, since
 * the tap answers what was typed the way a submitted line does.
 */
@Composable
public fun LauncherApp(
    homeState: HomeUiState,
    settingsState: SettingsUiState,
    settingsActions: SettingsActions,
    promptActions: PromptActions,
    submittedActions: Flow<SubmittedAction>,
    onLaunchApp: (InstalledApp) -> Unit,
    onLaunchShortcut: (AppShortcut) -> Unit,
    onRowStart: () -> Unit,
    onLockScreen: () -> Unit,
    onOpenSystemScreen: (SystemScreen) -> Unit,
    onRestartLauncher: () -> Unit,
) {
    var destination by rememberSaveable { mutableStateOf(LauncherDestination.HOME) }
    val launchApp by rememberUpdatedState(onLaunchApp)
    val openSystemScreen by rememberUpdatedState(onOpenSystemScreen)
    val restartLauncher by rememberUpdatedState(onRestartLauncher)

    LaunchedEffect(submittedActions) {
        submittedActions.collect { action ->
            when (action) {
                is SubmittedAction.LaunchApp -> launchApp(action.app)
                SubmittedAction.OpenSettings -> destination = LauncherDestination.SETTINGS
                is SubmittedAction.OpenSystemScreen -> openSystemScreen(action.screen)
                SubmittedAction.RestartLauncher -> restartLauncher()
            }
        }
    }

    TerminalThemeProvider(theme = settingsState.terminalTheme) {
        BackHandler(enabled = destination == LauncherDestination.SETTINGS) {
            destination = LauncherDestination.HOME
        }

        if (destination == LauncherDestination.HOME) {
            HomeScreen(
                state = homeState,
                onAppClick = { app ->
                    onRowStart()
                    onLaunchApp(app)
                },
                onShortcutClick = { shortcut ->
                    onRowStart()
                    onLaunchShortcut(shortcut)
                },
                onLockScreen = onLockScreen,
                promptActions = promptActions,
            )
        } else {
            SettingsScreen(
                state = settingsState,
                actions = settingsActions,
                onBack = { destination = LauncherDestination.HOME },
            )
        }
    }
}

private enum class LauncherDestination {
    HOME,
    SETTINGS,
}
