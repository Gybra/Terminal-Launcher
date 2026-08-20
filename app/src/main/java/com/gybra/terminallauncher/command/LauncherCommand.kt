package com.gybra.terminallauncher.command

/**
 * An explicitly registered launcher command. Implementations map to controlled Android
 * functionality; the launcher never executes shell input.
 */
public interface LauncherCommand {
    /** Stable identifier each shell profile maps to its own aliases. */
    public val id: Command

    public fun execute(context: CommandContext): CommandResult
}
