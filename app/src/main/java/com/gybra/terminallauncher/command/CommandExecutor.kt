package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.shell.ShellProfile

/** Runs submitted prompt input against the registered commands. */
public class CommandExecutor(
    private val registry: CommandRegistry,
) {
    /**
     * Runs the command [input] names in [shellProfile] and returns its result. Blank input and
     * unregistered names resolve to [CommandResult.Search] instead of running anything.
     */
    public fun execute(
        input: String,
        shellProfile: ShellProfile,
        installedApps: List<InstalledApp>,
    ): CommandResult {
        val tokens = CommandParser.tokenize(input)
        val name = tokens.firstOrNull() ?: return CommandResult.Search
        val command = registry.resolve(name = name, shellProfile = shellProfile)
            ?: return CommandResult.Search
        return command.execute(
            CommandContext(
                arguments = tokens.drop(1),
                shellProfile = shellProfile,
                installedApps = installedApps,
                registeredCommands = registry.summaries,
            ),
        )
    }
}
