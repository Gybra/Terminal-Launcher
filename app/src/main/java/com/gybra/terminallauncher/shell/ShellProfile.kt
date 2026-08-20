package com.gybra.terminallauncher.shell

import com.gybra.terminallauncher.command.Command
import com.gybra.terminallauncher.command.CommandSummary
import com.gybra.terminallauncher.launcher.InstalledApp

public interface ShellProfile {
    public val type: ShellType

    public fun prompt(context: ShellContext): String

    public fun formatAppName(app: InstalledApp): String

    public fun formatPath(location: LauncherLocation): String

    public fun aliasFor(command: Command): String

    public fun formatAppList(apps: List<InstalledApp>): List<String> = apps.map(::formatAppName)

    /**
     * Describes [commands] with the primary alias of this shell, so optional aliases accepted only
     * for compatibility stay out of the help output.
     */
    public fun formatHelp(commands: List<CommandSummary>): List<String> = commands.map { command ->
        aliasFor(command.id).padEnd(HELP_ALIAS_COLUMN_WIDTH) + command.description
    }

    public fun aliasesFor(command: Command): Set<String> = setOf(aliasFor(command))
}

private const val HELP_ALIAS_COLUMN_WIDTH = 10
