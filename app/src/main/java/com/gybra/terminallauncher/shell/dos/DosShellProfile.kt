package com.gybra.terminallauncher.shell.dos

import com.gybra.terminallauncher.command.Command
import com.gybra.terminallauncher.command.CommandSummary
import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.launcher.PinnedShortcut
import com.gybra.terminallauncher.shell.LauncherLocation
import com.gybra.terminallauncher.shell.ShellContext
import com.gybra.terminallauncher.shell.ShellProfile
import com.gybra.terminallauncher.shell.ShellType
import java.util.Locale

public object DosShellProfile : ShellProfile {
    override val type: ShellType = ShellType.DOS

    override fun prompt(context: ShellContext): String = "${formatPath(context)}>"

    override fun formatAppName(app: InstalledApp): String =
        "${app.label.uppercase(Locale.ROOT)}.EXE"

    /** A shortcut is written as the link file DOS would have kept it in. */
    override fun formatShortcutName(shortcut: PinnedShortcut): String =
        "${shortcut.label.uppercase(Locale.ROOT)}.LNK"

    override fun formatMessage(message: String): String = message.uppercase(Locale.ROOT)

    override fun formatPath(context: ShellContext): String {
        val root = "${context.dosDrive}:\\"
        if (!context.showPath) {
            return root
        }

        return root + when (context.location) {
            LauncherLocation.HOME -> "HOME"
            LauncherLocation.APPS -> "APPS"
        }
    }

    override fun aliasFor(command: Command): String = when (command) {
        Command.LIST_APPS -> "DIR"
        Command.CLEAR -> "CLS"
        Command.HELP -> "HELP"
        Command.PIN -> "PIN"
        Command.UNPIN -> "UNPIN"
        Command.SETTINGS -> "SETTINGS"
        Command.ALIAS -> "ALIAS"
        Command.BATTERY -> "BATTERY"
        Command.TORCH -> "TORCH"
        Command.APP_INFO -> "INFO"
        Command.UNINSTALL -> "UNINSTALL"
        Command.ANDROID_SETTINGS -> "ANDROID"
        Command.WIFI_SETTINGS -> "WIFI"
        Command.BLUETOOTH_SETTINGS -> "BLUETOOTH"
        Command.RESTART -> "RESTART"
    }

    override fun formatAppList(apps: List<InstalledApp>): List<String> =
        apps.map(::formatAppName) + listOf("", "${apps.size} File(s)")

    override fun formatHelp(commands: List<CommandSummary>): List<String> =
        super.formatHelp(commands) + listOf("", "${commands.size} Command(s)")
}
