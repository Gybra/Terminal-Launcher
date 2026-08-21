package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.search.AppSearchEngine
import com.gybra.terminallauncher.search.SearchResult
import com.gybra.terminallauncher.shell.ShellProfile

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
    val app = AppSearchEngine.unambiguousMatch(results)
        ?: return shellProfile.rejectAppQuery(query = query, results = results)

    return action(app)
}

/**
 * Answers a [query] that named no single application among [results]. The prompt writes the same
 * answer when a submitted line resolves to nothing, so both report an unresolved name once, and
 * the candidates are carried rather than written out so they stay startable.
 */
internal fun ShellProfile.rejectAppQuery(
    query: String,
    results: List<SearchResult>,
): CommandResult.Listing {
    val answer = if (results.isEmpty()) {
        "no application matches $query"
    } else {
        "$query matches more than one application"
    }

    return CommandResult.Listing(
        lines = listOf(formatMessage(answer)),
        apps = results.map(SearchResult::app),
    )
}
