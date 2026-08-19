package com.gybra.terminallauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.ui.home.HomeScreen
import com.gybra.terminallauncher.ui.home.HomeUiState
import com.gybra.terminallauncher.ui.settings.SettingsActions
import com.gybra.terminallauncher.ui.settings.SettingsScreen
import com.gybra.terminallauncher.ui.settings.SettingsUiState

@Composable
public fun LauncherApp(
    homeState: HomeUiState,
    settingsState: SettingsUiState,
    settingsActions: SettingsActions,
    onAppClick: (InstalledApp) -> Unit,
) {
    var destination by rememberSaveable { mutableStateOf(LauncherDestination.HOME) }

    BackHandler(enabled = destination == LauncherDestination.SETTINGS) {
        destination = LauncherDestination.HOME
    }

    if (destination == LauncherDestination.HOME) {
        HomeScreen(
            state = homeState,
            onAppClick = onAppClick,
            onSettingsClick = { destination = LauncherDestination.SETTINGS },
        )
    } else {
        SettingsScreen(
            state = settingsState,
            onShellSelected = settingsActions.selectShell,
            onShowClockChanged = settingsActions.setShowClock,
            onUsernameChanged = settingsActions.setUsername,
            onHostnameChanged = settingsActions.setHostname,
            onBack = { destination = LauncherDestination.HOME },
        )
    }
}

private enum class LauncherDestination {
    HOME,
    SETTINGS,
}
