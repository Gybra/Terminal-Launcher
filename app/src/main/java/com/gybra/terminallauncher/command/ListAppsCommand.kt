package com.gybra.terminallauncher.command

/** Lists installed applications the way the active shell profile writes them. */
public object ListAppsCommand : LauncherCommand {
    override val id: Command = Command.LIST_APPS

    override val group: CommandGroup = CommandGroup.APPS

    override val description: String = "List installed apps"

    override suspend fun execute(context: CommandContext): CommandResult =
        CommandResult.Output(context.shellProfile.formatAppList(context.installedApps))
}
