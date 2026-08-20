package com.gybra.terminallauncher.search

import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.search.SearchResult.Match
import java.util.Locale

/** Ranks installed applications against what the user is typing at the prompt. */
public object AppSearchEngine {
    /** Highest number of results a query may produce. */
    public const val MAX_RESULTS: Int = 5

    /**
     * Matches [apps] against [query] on their label, ignoring case and surrounding whitespace.
     * Exact matches rank first, then prefix matches, then substring matches; results sharing a
     * match strength stay ordered by label and package name so the list never reorders itself
     * for the same input. A blank query matches nothing.
     */
    public fun search(query: String, apps: List<InstalledApp>): List<SearchResult> {
        val normalizedQuery = query.trim().lowercase(Locale.ROOT)
        if (normalizedQuery.isEmpty()) {
            return emptyList()
        }

        return apps
            .mapNotNull { app -> app.matchAgainst(normalizedQuery) }
            .sortedWith(RESULT_ORDER)
            .take(MAX_RESULTS)
    }

    /**
     * Returns the application [results] point to without guessing: the only result, or the only
     * exact match among several. Anything else is ambiguous and resolves to `null` so the caller
     * keeps every match visible.
     */
    public fun unambiguousMatch(results: List<SearchResult>): InstalledApp? {
        val resolved = results.singleOrNull()
            ?: results.singleOrNull { result -> result.match == Match.EXACT }
        return resolved?.app
    }

    private fun InstalledApp.matchAgainst(query: String): SearchResult? {
        val normalizedLabel = label.lowercase(Locale.ROOT)
        val match = when {
            normalizedLabel == query -> Match.EXACT
            normalizedLabel.startsWith(query) -> Match.PREFIX
            normalizedLabel.contains(query) -> Match.SUBSTRING
            else -> null
        }
        return match?.let { strength -> SearchResult(app = this, match = strength) }
    }

    private val RESULT_ORDER = compareBy<SearchResult>(SearchResult::match)
        .thenBy { result -> result.app.label.lowercase(Locale.ROOT) }
        .thenBy { result -> result.app.packageName }
}
