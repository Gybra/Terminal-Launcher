package com.gybra.terminallauncher.shell

import com.gybra.terminallauncher.command.Command
import com.gybra.terminallauncher.command.CommandSummary
import com.gybra.terminallauncher.launcher.BatteryStatus
import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.launcher.PinnedShortcut

public interface ShellProfile {
    public val type: ShellType

    public fun prompt(context: ShellContext): String

    public fun formatAppName(app: InstalledApp): String

    /** Writes the name of a shortcut pinned to Home, which no application list ever contains. */
    public fun formatShortcutName(shortcut: PinnedShortcut): String

    /** Writes the location [context] points at, whether or not the prompt shows it. */
    public fun formatPath(context: ShellContext): String

    public fun aliasFor(command: Command): String

    /** Writes a one-line command message, such as a confirmation or an error, in this shell style. */
    public fun formatMessage(message: String): String

    public fun formatAppList(apps: List<InstalledApp>): List<String> = apps.map(::formatAppName)

    /**
     * Describes [commands] with the primary alias of this shell, so optional aliases accepted only
     * for compatibility stay out of the help output.
     */
    public fun formatHelp(commands: List<CommandSummary>): List<String> = commands.map { command ->
        aliasFor(command.id).padEnd(HELP_ALIAS_COLUMN_WIDTH) + command.description
    }

    public fun aliasesFor(command: Command): Set<String> = setOf(aliasFor(command))

    /** Writes the battery part of the line Home keeps above everything else. */
    public fun formatBattery(battery: BatteryStatus): String = formatMessage(
        "${battery.percentage}%" + if (battery.charging) " charging" else "",
    )
}

private const val HELP_ALIAS_COLUMN_WIDTH = 10
