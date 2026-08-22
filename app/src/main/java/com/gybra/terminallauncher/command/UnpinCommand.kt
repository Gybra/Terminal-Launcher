package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.preferences.PreferencesRepository

/** Removes the application named by its argument from Home. */
public class UnpinCommand(
    private val preferencesRepository: PreferencesRepository,
) : PinningCommand() {
    override val id: Command = Command.UNPIN

    override val group: CommandGroup = CommandGroup.HOME

    override val description: String = "Remove an app from Home"

    override val outcome: String = "unpinned"

    override suspend fun changePinning(app: InstalledApp) {
        preferencesRepository.unpinPackage(app.packageName)
    }
}
