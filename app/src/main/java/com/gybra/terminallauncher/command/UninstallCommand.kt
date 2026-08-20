package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.launcher.SystemScreen

/**
 * Asks Android to uninstall the application the argument names. The launcher removes nothing
 * itself: Android shows its own confirmation, naming the application, and the user answers it.
 */
public object UninstallCommand : ApplicationCommand() {
    override val id: Command = Command.UNINSTALL

    override val description: String = "Ask Android to uninstall an application"

    override suspend fun apply(app: InstalledApp, context: CommandContext): CommandResult =
        CommandResult.OpenSystemScreen(SystemScreen.Uninstall(app.packageName))
}
