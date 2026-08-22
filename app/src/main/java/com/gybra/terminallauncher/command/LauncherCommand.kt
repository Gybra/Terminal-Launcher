package com.gybra.terminallauncher.command

/**
 * An explicitly registered launcher command. Implementations map to controlled Android
 * functionality; the launcher never executes shell input.
 */
public interface LauncherCommand {
    /** Stable identifier each shell profile maps to its own aliases. */
    public val id: Command

    /** What this command acts on, which is the group help writes it under. */
    public val group: CommandGroup

    /** Shell-independent metadata the help command turns into a description line. */
    public val description: String

    /**
     * The argument forms this command accepts, written after its alias, such as `<application>`.
     * Help and the answer to a wrong invocation are both written from them, so a command spells
     * the way it is called exactly once. A command taking no argument leaves them empty.
     */
    public val usage: List<String>
        get() = emptyList()

    public suspend fun execute(context: CommandContext): CommandResult
}
