package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.preferences.PreferencesRepository

/**
 * Every command the launcher registers, in the order help lists them. The composition root and
 * the tests share this list, so what ships is what is verified.
 */
public fun launcherCommands(
    preferencesRepository: PreferencesRepository,
): List<LauncherCommand> = listOf(
    ListAppsCommand,
    HelpCommand,
    PinCommand(preferencesRepository),
    UnpinCommand(preferencesRepository),
    ClearCommand,
    SettingsCommand,
)
