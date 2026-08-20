package com.gybra.terminallauncher.command

/** Describes the registered commands, letting the active shell profile write the lines. */
public object HelpCommand : LauncherCommand {
    override val id: Command = Command.HELP

    override val description: String = "Show available commands"

    override suspend fun execute(context: CommandContext): CommandResult =
        CommandResult.Output(context.shellProfile.formatHelp(context.registeredCommands))
}
