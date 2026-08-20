package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.launcher.InstalledApp

/** Shared behavior of the commands that change the pinned applications. */
public abstract class PinningCommand : ApplicationCommand() {
    /** Word the success message is written with, such as `pinned`. */
    protected abstract val outcome: String

    /** Applies the change once the argument resolved to a single [app]. */
    protected abstract suspend fun changePinning(app: InstalledApp)

    final override suspend fun apply(app: InstalledApp, context: CommandContext): CommandResult {
        changePinning(app)

        return context.message("$outcome ${context.shellProfile.formatAppName(app)}")
    }
}
