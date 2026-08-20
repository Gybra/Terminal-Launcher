package com.gybra.terminallauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.ui.home.HomeScreen
import com.gybra.terminallauncher.ui.home.HomeUiState
import com.gybra.terminallauncher.ui.home.PromptActions
import com.gybra.terminallauncher.ui.home.SubmittedAction
import com.gybra.terminallauncher.ui.settings.SettingsActions
import com.gybra.terminallauncher.ui.settings.SettingsScreen
import com.gybra.terminallauncher.ui.settings.SettingsUiState
import com.gybra.terminallauncher.ui.theme.TerminalThemeProvider
import kotlinx.coroutines.flow.Flow

@Composable
public fun LauncherApp(
    homeState: HomeUiState,
    settingsState: SettingsUiState,
    settingsActions: SettingsActions,
    promptActions: PromptActions,
    submittedActions: Flow<SubmittedAction>,
    onLaunchApp: (InstalledApp) -> Unit,
) {
    var destination by rememberSaveable { mutableStateOf(LauncherDestination.HOME) }

    LaunchedEffect(submittedActions) {
        submittedActions.collect { action ->
            when (action) {
                is SubmittedAction.LaunchApp -> onLaunchApp(action.app)
                SubmittedAction.OpenSettings -> destination = LauncherDestination.SETTINGS
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
                onAppClick = onLaunchApp,
                onSettingsClick = { destination = LauncherDestination.SETTINGS },
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
