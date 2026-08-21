package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.launcher.BatteryRepository
import com.gybra.terminallauncher.launcher.ShortcutRepository
import com.gybra.terminallauncher.launcher.SystemScreen
import com.gybra.terminallauncher.launcher.Torch
import com.gybra.terminallauncher.preferences.PreferencesRepository

/**
 * Every command the launcher registers, in the order help lists them. The composition root and
 * the tests share this list, so what ships is what is verified.
 */
public fun launcherCommands(
    preferencesRepository: PreferencesRepository,
    batteryRepository: BatteryRepository,
    shortcutRepository: ShortcutRepository,
    torch: Torch,
): List<LauncherCommand> = listOf(
    ListAppsCommand,
    HelpCommand,
    PinCommand(preferencesRepository),
    UnpinCommand(preferencesRepository),
    ShortcutsCommand(
        shortcutRepository = shortcutRepository,
        preferencesRepository = preferencesRepository,
    ),
    AliasCommand(preferencesRepository),
    AppInfoCommand,
    UninstallCommand,
    BatteryCommand(batteryRepository),
    TorchCommand(torch),
    SystemScreenCommand(
        id = Command.ANDROID_SETTINGS,
        description = "Open the Android settings",
        screen = SystemScreen.AndroidSettings,
    ),
    SystemScreenCommand(
        id = Command.WIFI_SETTINGS,
        description = "Open the Wi-Fi settings",
        screen = SystemScreen.WifiSettings,
    ),
    SystemScreenCommand(
        id = Command.BLUETOOTH_SETTINGS,
        description = "Open the Bluetooth settings",
        screen = SystemScreen.BluetoothSettings,
    ),
    ClearCommand,
    RestartCommand,
    SettingsCommand,
)
