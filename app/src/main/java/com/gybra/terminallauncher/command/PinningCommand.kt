package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.search.AppSearchEngine
import com.gybra.terminallauncher.search.SearchResult

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

        val results = AppSearchEngine.search(query = query, apps = context.installedApps)
        val app = AppSearchEngine.unambiguousMatch(results)
            ?: return context.reject(query = query, results = results)

        apply(app)
        return context.message("$outcome ${context.shellProfile.formatAppName(app)}")
    }

    private fun CommandContext.reject(query: String, results: List<SearchResult>): CommandResult {
        if (results.isEmpty()) {
            return message("no application matches $query")
        }

        return CommandResult.Output(
            listOf(shellProfile.formatMessage("$query matches more than one application")) +
                shellProfile.formatAppList(results.map(SearchResult::app)),
        )
    }

    private fun CommandContext.message(text: String): CommandResult =
        CommandResult.Output(listOf(shellProfile.formatMessage(text)))
}
