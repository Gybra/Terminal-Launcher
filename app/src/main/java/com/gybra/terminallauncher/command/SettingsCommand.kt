package com.gybra.terminallauncher.command

/** Opens the settings destination the Home list already links to. */
public object SettingsCommand : LauncherCommand {
    override val id: Command = Command.SETTINGS

    override val description: String = "Open the launcher settings"

    override suspend fun execute(context: CommandContext): CommandResult = CommandResult.OpenSettings
}
