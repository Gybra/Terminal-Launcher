package com.gybra.terminallauncher.shell.unix

import com.gybra.terminallauncher.command.Command
import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.launcher.AppShortcut
import com.gybra.terminallauncher.shell.LauncherLocation
import com.gybra.terminallauncher.shell.ShellContext
import com.gybra.terminallauncher.shell.ShellProfile
import com.gybra.terminallauncher.shell.ShellType
import java.util.Locale

public object UnixShellProfile : ShellProfile {
    override val type: ShellType = ShellType.UNIX

    /**
     * Writes `username@hostname:path` followed by the chosen symbol, leaving out every part that
     * is empty or hidden, so an identity cleared in the settings never leaves a dangling
     * separator behind.
     */
    override fun prompt(context: ShellContext): String {
        val identity = listOf(context.username, context.hostname)
            .filter(String::isNotEmpty)
            .joinToString(separator = IDENTITY_SEPARATOR)
        val path = if (context.showPath) formatPath(context) else ""

        return listOf(identity, path)
            .filter(String::isNotEmpty)
            .joinToString(separator = PATH_SEPARATOR) + context.promptSymbol.text
    }

    override fun formatAppName(app: InstalledApp): String = app.label.lowercase(Locale.ROOT)

    override fun formatShortcutName(shortcut: AppShortcut): String =
        shortcut.label.lowercase(Locale.ROOT)

    override fun formatMessage(message: String): String = message.lowercase(Locale.ROOT)

    override fun formatPath(context: ShellContext): String = when (context.location) {
        LauncherLocation.HOME -> "~"
        LauncherLocation.APPS -> "~/apps"
    }

    override fun aliasFor(command: Command): String = when (command) {
        Command.LIST_APPS -> "ls"
        Command.CLEAR -> "clear"
        Command.HELP -> "help"
        Command.PIN -> "pin"
        Command.UNPIN -> "unpin"
        Command.SETTINGS -> "settings"
        Command.ALIAS -> "alias"
        Command.BATTERY -> "battery"
        Command.TORCH -> "torch"
        Command.APP_INFO -> "info"
        Command.UNINSTALL -> "uninstall"
        Command.ANDROID_SETTINGS -> "android"
        Command.WIFI_SETTINGS -> "wifi"
        Command.BLUETOOTH_SETTINGS -> "bluetooth"
        Command.RESTART -> "restart"
    }

    override fun aliasesFor(command: Command): Set<String> = when (command) {
        Command.LIST_APPS -> setOf("ls", "dir")
        Command.CLEAR -> setOf("clear", "cls")
        else -> setOf(aliasFor(command))
    }

    private const val IDENTITY_SEPARATOR = "@"

    private const val PATH_SEPARATOR = ":"
}
