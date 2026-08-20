package com.gybra.terminallauncher.command

/**
 * Starts the launcher again, so Home is rebuilt from the stored preferences. Nothing is killed
 * and no process is spawned: Android recreates the launcher the way a configuration change does.
 */
public object RestartCommand : LauncherCommand {
    override val id: Command = Command.RESTART

    override val description: String = "Restart the launcher"

    override suspend fun execute(context: CommandContext): CommandResult =
        CommandResult.RestartLauncher
}
