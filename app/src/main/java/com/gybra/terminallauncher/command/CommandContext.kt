package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.shell.ShellProfile

/** What a [LauncherCommand] receives when the submitted prompt input resolves to it. */
public data class CommandContext(
    public val arguments: List<String>,
    public val shellProfile: ShellProfile,
    public val installedApps: List<InstalledApp>,
    public val registeredCommands: List<CommandSummary>,
)
