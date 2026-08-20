package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.launcher.SystemScreen

/** Opens one fixed system destination, chosen when the command is registered. */
public class SystemScreenCommand(
    override val id: Command,
    override val description: String,
    private val screen: SystemScreen,
) : LauncherCommand {
    override suspend fun execute(context: CommandContext): CommandResult =
        CommandResult.OpenSystemScreen(screen)
}
