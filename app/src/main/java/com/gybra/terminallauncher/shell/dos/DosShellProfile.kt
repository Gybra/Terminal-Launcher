package com.gybra.terminallauncher.shell.dos

import com.gybra.terminallauncher.command.Command
import com.gybra.terminallauncher.command.CommandSummary
import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.shell.LauncherLocation
import com.gybra.terminallauncher.shell.ShellContext
import com.gybra.terminallauncher.shell.ShellProfile
import com.gybra.terminallauncher.shell.ShellType
import java.util.Locale

public object DosShellProfile : ShellProfile {
    override val type: ShellType = ShellType.DOS

    override fun prompt(context: ShellContext): String = "${formatPath(context.location)}>"

    override fun formatAppName(app: InstalledApp): String =
        "${app.label.uppercase(Locale.ROOT)}.EXE"

    override fun formatMessage(message: String): String = message.uppercase(Locale.ROOT)

    override fun formatPath(location: LauncherLocation): String = when (location) {
        LauncherLocation.HOME -> "C:\\HOME"
        LauncherLocation.APPS -> "C:\\APPS"
    }

    override fun aliasFor(command: Command): String = when (command) {
        Command.LIST_APPS -> "DIR"
        Command.CLEAR -> "CLS"
        Command.HELP -> "HELP"
        Command.PIN -> "PIN"
        Command.UNPIN -> "UNPIN"
        Command.SETTINGS -> "SETTINGS"
    }

    override fun formatAppList(apps: List<InstalledApp>): List<String> =
        apps.map(::formatAppName) + listOf("", "${apps.size} File(s)")

    override fun formatHelp(commands: List<CommandSummary>): List<String> =
        super.formatHelp(commands) + listOf("", "${commands.size} Command(s)")
}
