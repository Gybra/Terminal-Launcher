package com.gybra.terminallauncher.shell.unix

import com.gybra.terminallauncher.command.Command
import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.shell.LauncherLocation
import com.gybra.terminallauncher.shell.ShellContext
import com.gybra.terminallauncher.shell.ShellProfile
import com.gybra.terminallauncher.shell.ShellType
import java.util.Locale

public object UnixShellProfile : ShellProfile {
    override val type: ShellType = ShellType.UNIX

    override fun prompt(context: ShellContext): String =
        "${context.username}@${context.hostname}:${formatPath(context.location)}$"

    override fun formatAppName(app: InstalledApp): String = app.label.lowercase(Locale.ROOT)

    override fun formatPath(location: LauncherLocation): String = when (location) {
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
    }

    override fun aliasesFor(command: Command): Set<String> = when (command) {
        Command.LIST_APPS -> setOf("ls", "dir")
        Command.CLEAR -> setOf("clear", "cls")
        else -> setOf(aliasFor(command))
    }
}
