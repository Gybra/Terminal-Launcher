package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.search.AppSearchEngine
import com.gybra.terminallauncher.search.SearchResult

/** Writes [text] as a single result line in the style of the running shell. */
internal fun CommandContext.message(text: String): CommandResult =
    CommandResult.Output(listOf(shellProfile.formatMessage(text)))

/**
 * Runs [action] with the one application [query] names. A query matching nothing or several
 * applications is reported instead of guessed, listing the candidates as [AppSearchEngine] ranks
 * them, so a query matching more applications than the engine returns asks for a longer name.
 */
internal suspend fun CommandContext.withResolvedApp(
    query: String,
    action: suspend (InstalledApp) -> CommandResult,
): CommandResult {
    val results = AppSearchEngine.search(query = query, apps = installedApps)
    val app = AppSearchEngine.unambiguousMatch(results) ?: return reject(query = query, results = results)

    return action(app)
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
