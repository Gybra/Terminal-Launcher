package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.launcher.InstalledApp

/**
 * Shared behavior of the commands that act on one installed application. The argument must resolve
 * to exactly one application; anything else is reported instead of guessed, and a missing argument
 * is answered with the usage line of the running shell.
 */
public abstract class ApplicationCommand : LauncherCommand {
    /** Acts on the [app] the argument resolved to and reports what the launcher does next. */
    protected abstract suspend fun apply(app: InstalledApp, context: CommandContext): CommandResult

    final override suspend fun execute(context: CommandContext): CommandResult {
        val query = context.arguments.joinToString(separator = " ")
        if (query.isBlank()) {
            return context.message("usage: ${context.shellProfile.aliasFor(id)} <application>")
        }

        return context.withResolvedApp(query) { app -> apply(app, context) }
    }
}
