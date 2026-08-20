package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.launcher.InstalledApp

/**
 * Shared behavior of the commands that change the pinned applications. The argument must resolve
 * to exactly one installed application; anything else is reported instead of guessed.
 */
public abstract class PinningCommand : LauncherCommand {
    /** Word the success message is written with, such as `pinned`. */
    protected abstract val outcome: String

    /** Applies the change once the argument resolved to a single [app]. */
    protected abstract suspend fun apply(app: InstalledApp)

    final override suspend fun execute(context: CommandContext): CommandResult {
        val query = context.arguments.joinToString(separator = " ")
        if (query.isBlank()) {
            return context.message("usage: ${context.shellProfile.aliasFor(id)} <application>")
        }

        return context.withResolvedApp(query) { app ->
            apply(app)
            context.message("$outcome ${context.shellProfile.formatAppName(app)}")
        }
    }
}
