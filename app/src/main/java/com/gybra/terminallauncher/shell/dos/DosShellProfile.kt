package com.gybra.terminallauncher.shell.dos

import com.gybra.terminallauncher.command.Command
import com.gybra.terminallauncher.command.CommandSummary
import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.launcher.AppShortcut
import com.gybra.terminallauncher.shell.LauncherLocation
import com.gybra.terminallauncher.shell.PromptCursor
import com.gybra.terminallauncher.shell.ShellContext
import com.gybra.terminallauncher.shell.PINNED_DIRECTORY
import com.gybra.terminallauncher.shell.SectionLines
import com.gybra.terminallauncher.shell.ShellProfile
import com.gybra.terminallauncher.shell.ShellType
import java.util.Locale

public object DosShellProfile : ShellProfile {
    override val type: ShellType = ShellType.DOS

    override val cursor: PromptCursor = PromptCursor.BLOCK

    override fun prompt(context: ShellContext): String = "${formatPath(context)}>"

    override fun formatAppName(app: InstalledApp): String =
        "${app.label.uppercase(Locale.ROOT)}.EXE"

    /** A shortcut is written as the link file DOS would have kept it in. */
    override fun formatShortcutName(shortcut: AppShortcut): String =
        "${shortcut.label.uppercase(Locale.ROOT)}.LNK"

    override fun formatMessage(message: String): String = message.uppercase(Locale.ROOT)

    override fun formatPath(context: ShellContext): String {
        val root = "${context.dosDrive}:$PATH_SEPARATOR"
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
        Command.SHORTCUTS -> "SHORTCUTS"
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
        apps.map(::formatAppName) + listOf("", fileCount(apps.size))

    /** Announces the pinned block the way DIR announces the directory it is about to write. */
    override fun formatPinnedSection(context: ShellContext, items: Int): SectionLines {
        val directory = formatPath(context.atHome()) + PATH_SEPARATOR + formatMessage(PINNED_DIRECTORY)

        return SectionLines(
            above = listOf("Directory of $directory", ""),
            below = listOf("", fileCount(items)),
        )
    }

    override fun formatHelp(commands: List<CommandSummary>): List<String> =
        super.formatHelp(commands) + listOf("", "${commands.size} Command(s)")

    /** Points [this] at Home with the path shown, which is where the pinned block always is. */
    private fun ShellContext.atHome(): ShellContext =
        copy(location = LauncherLocation.HOME, showPath = true)

    private fun fileCount(files: Int): String = "$files File(s)"

    private const val PATH_SEPARATOR = "\\"
}
