package com.gybra.terminallauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.launcher.AppShortcut
import com.gybra.terminallauncher.launcher.SystemScreen
import com.gybra.terminallauncher.ui.home.HomeScreen
import com.gybra.terminallauncher.ui.home.HomeUiState
import com.gybra.terminallauncher.ui.home.PromptActions
import com.gybra.terminallauncher.ui.home.SubmittedAction
import com.gybra.terminallauncher.ui.home.rememberPromptRelease
import com.gybra.terminallauncher.ui.privacy.AccessibilityDisclosure
import com.gybra.terminallauncher.ui.privacy.PrivacyPolicyScreen
import com.gybra.terminallauncher.ui.settings.SettingsActions
import com.gybra.terminallauncher.ui.settings.SettingsScreen
import com.gybra.terminallauncher.ui.settings.SettingsUiState
import com.gybra.terminallauncher.ui.theme.TerminalThemeProvider
import kotlinx.coroutines.flow.Flow

/**
 * Shows Home or the settings, and turns what a row or a submitted line asks into the launcher
 * work the composition root wired. Starting anything from a row calls [onRowStart] first, since
 * the tap answers what was typed the way a submitted line does. Every start also releases the
 * prompt, because Home hands the screen over and must not carry a live keyboard through it, and
 * so does anything else taking the screen, since Android restores the keyboard for a field that
 * is still focused when the launcher comes back.
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
    onExpandNotifications: () -> Unit = {},
    onExpandQuickSettings: () -> Unit = {},
    onOpenOverview: () -> Unit = {},
) {
    var destination by rememberSubmittedActionDestination(
        submittedActions, onLaunchApp, onOpenSystemScreen, onRestartLauncher,
    )
    val releasePrompt = rememberPromptRelease()
    // External app starts, recents and locking also leave Home; none may restore a live keyboard.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { releasePrompt() }

    LaunchedEffect(settingsState.accessibilityDisclosureVisible) {
        if (settingsState.accessibilityDisclosureVisible) releasePrompt()
    }

    TerminalThemeProvider(theme = settingsState.terminalTheme) {
        // Home is the root of the launcher task, so a Back nothing else answered would finish the
        // activity and leave the system to start Home again, losing the terminal history with it.
        // Registered first, so the settings below and the prompt inside Home are asked before it.
        BackHandler(enabled = true) {}

        BackHandler(enabled = destination != LauncherDestination.HOME) {
            destination = if (destination == LauncherDestination.PRIVACY) {
                LauncherDestination.SETTINGS
            } else {
                LauncherDestination.HOME
            }
        }

        if (destination == LauncherDestination.HOME) {
            HomeScreen(
                state = homeState,
                onAppClick = { app ->
                    releasePrompt()
                    onRowStart()
                    onLaunchApp(app)
                },
                onShortcutClick = { shortcut ->
                    releasePrompt()
                    onRowStart()
                    onLaunchShortcut(shortcut)
                },
                onLockScreen = onLockScreen,
                onExpandNotifications = onExpandNotifications,
                onExpandQuickSettings = onExpandQuickSettings,
                onOpenOverview = onOpenOverview,
                promptActions = promptActions,
            )
        } else if (destination == LauncherDestination.SETTINGS) {
            SettingsScreen(
                state = settingsState,
                actions = settingsActions,
                onBack = { destination = LauncherDestination.HOME },
                onOpenPrivacy = { destination = LauncherDestination.PRIVACY },
            )
        } else {
            PrivacyPolicyScreen(onBack = { destination = LauncherDestination.SETTINGS })
        }
        if (settingsState.accessibilityDisclosureVisible) {
            AccessibilityDisclosure(
                onAccept = settingsActions.acceptAccessibilityDisclosure,
                onDismiss = settingsActions.dismissAccessibilityDisclosure,
                saving = settingsState.savingAccessibilityConsent,
                error = settingsState.storageError,
            )
        }
    }
}

@Composable
private fun rememberSubmittedActionDestination(
    submittedActions: Flow<SubmittedAction>,
    onLaunchApp: (InstalledApp) -> Unit,
    onOpenSystemScreen: (SystemScreen) -> Unit,
    onRestartLauncher: () -> Unit,
): MutableState<LauncherDestination> {
    val destination = rememberSaveable { mutableStateOf(LauncherDestination.HOME) }
    val releasePrompt = rememberPromptRelease()
    val launchApp by rememberUpdatedState(onLaunchApp)
    val openSystemScreen by rememberUpdatedState(onOpenSystemScreen)
    val restartLauncher by rememberUpdatedState(onRestartLauncher)
    LaunchedEffect(submittedActions) {
        submittedActions.collect { action ->
            releasePrompt()
            when (action) {
                is SubmittedAction.LaunchApp -> launchApp(action.app)
                SubmittedAction.OpenSettings -> destination.value = LauncherDestination.SETTINGS
                is SubmittedAction.OpenSystemScreen -> openSystemScreen(action.screen)
                SubmittedAction.RestartLauncher -> restartLauncher()
            }
        }
    }
    return destination
}

private enum class LauncherDestination {
    HOME,
    SETTINGS,
    PRIVACY,
}
