package com.gybra.terminallauncher.command

/** Erases the terminal history and nothing else. */
public object ClearCommand : LauncherCommand {
    override val id: Command = Command.CLEAR

    override val group: CommandGroup = CommandGroup.LAUNCHER

    override val description: String = "Clear the history"

    override suspend fun execute(context: CommandContext): CommandResult = CommandResult.ClearHistory
}
