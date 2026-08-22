package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.preferences.PreferencesRepository

/** Pins the application named by its argument to Home. */
public class PinCommand(
    private val preferencesRepository: PreferencesRepository,
) : PinningCommand() {
    override val id: Command = Command.PIN

    override val group: CommandGroup = CommandGroup.HOME

    override val description: String = "Pin an app to Home"

    override val outcome: String = "pinned"

    override suspend fun changePinning(app: InstalledApp) {
        preferencesRepository.pinPackage(app.packageName)
    }
}
