package com.gybra.terminallauncher.command

/** Erases the terminal history and nothing else. */
public object ClearCommand : LauncherCommand {
    override val id: Command = Command.CLEAR

    override val description: String = "Clear the terminal history"

    override fun execute(context: CommandContext): CommandResult = CommandResult.ClearHistory
}
