package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.launcher.SystemScreen

/** Opens the Android details of the application the argument names. */
public object AppInfoCommand : ApplicationCommand() {
    override val id: Command = Command.APP_INFO

    override val description: String = "Open the Android details of an application"

    override suspend fun apply(app: InstalledApp, context: CommandContext): CommandResult =
        CommandResult.OpenSystemScreen(SystemScreen.ApplicationDetails(app.packageName))
}
