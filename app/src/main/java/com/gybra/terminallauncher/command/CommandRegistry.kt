package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.shell.ShellProfile
import java.util.Locale

/**
 * Holds every command the launcher may run. Commands are registered here explicitly, so input
 * that resolves to nothing can never reach an unregistered code path.
 */
public class CommandRegistry(
    private val commands: List<LauncherCommand>,
) {
    init {
        commands.groupingBy(LauncherCommand::id).eachCount().forEach { (id, registrations) ->
            require(registrations == 1) { "Duplicate command registration: $id" }
        }
    }

    /**
     * Returns the command [name] stands for in [shellProfile], comparing aliases without case,
     * or `null` when the shell knows no such command.
     */
    public fun resolve(name: String, shellProfile: ShellProfile): LauncherCommand? {
        val normalizedName = name.lowercase(Locale.ROOT)
        return commands.firstOrNull { command ->
            shellProfile.aliasesFor(command.id).any { alias ->
                alias.lowercase(Locale.ROOT) == normalizedName
            }
        }
    }
}
