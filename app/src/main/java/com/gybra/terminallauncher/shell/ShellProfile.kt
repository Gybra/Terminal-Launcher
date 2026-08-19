package com.gybra.terminallauncher.shell

import com.gybra.terminallauncher.command.Command
import com.gybra.terminallauncher.launcher.InstalledApp

public interface ShellProfile {
    public val type: ShellType

    public fun prompt(context: ShellContext): String

    public fun formatAppName(app: InstalledApp): String

    public fun formatPath(location: LauncherLocation): String

    public fun aliasFor(command: Command): String

    public fun formatAppList(apps: List<InstalledApp>): List<String> = apps.map(::formatAppName)

    public fun aliasesFor(command: Command): Set<String> = setOf(aliasFor(command))

    public fun commandForAlias(alias: String): Command? = Command.entries.firstOrNull { command ->
        aliasesFor(command).any { candidate -> candidate.equals(alias, ignoreCase = true) }
    }
}
