package com.gybra.terminallauncher.search

import com.gybra.terminallauncher.launcher.AppUsage
import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.search.SearchResult.Match
import java.util.Locale

/** Ranks installed applications against what the user is typing at the prompt. */
public object AppSearchEngine {
    /** Highest number of results a query may produce. */
    public const val MAX_RESULTS: Int = 5

    /**
     * Matches [apps] against [query] on their label, ignoring case and surrounding whitespace.
     *
     * Match strength ranks first, so a literal match always beats a fuzzy one: exact, then prefix,
     * then substring, then a fuzzy match whose query characters appear in the label in order.
     * Equally strong matches are ordered by a score of 50 for a pinned package plus one point per
     * launch up to 20, then by the most recent launch, then by label and package name. Ranking
     * reads only [apps], [usage] and [pinnedPackages], so the same input always produces the same
     * list. A blank query matches nothing.
     */
    public fun search(
        query: String,
        apps: List<InstalledApp>,
        usage: Map<String, AppUsage> = emptyMap(),
        pinnedPackages: Set<String> = emptySet(),
    ): List<SearchResult> {
        val normalizedQuery = query.trim().lowercase(Locale.ROOT)
        if (normalizedQuery.isEmpty()) {
            return emptyList()
        }

        return apps
            .mapNotNull { app -> app.matchAgainst(normalizedQuery) }
            .sortedWith(resultOrder(usage, pinnedPackages))
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
            normalizedLabel.containsInOrder(query) -> Match.FUZZY
            else -> null
        }
        return match?.let { strength -> SearchResult(app = this, match = strength) }
    }

    /** True when every character of [query] appears in this label in order, gaps allowed. */
    private fun String.containsInOrder(query: String): Boolean {
        var matched = 0
        for (character in this) {
            if (character == query[matched]) {
                matched++
                if (matched == query.length) {
                    return true
                }
            }
        }
        return false
    }

    private fun resultOrder(
        usage: Map<String, AppUsage>,
        pinnedPackages: Set<String>,
    ): Comparator<SearchResult> = compareBy<SearchResult>(SearchResult::match)
        .thenByDescending { result -> result.app.rankingScore(usage, pinnedPackages) }
        .thenByDescending { result -> usage[result.app.packageName]?.lastLaunchedAt ?: 0L }
        .thenBy { result -> result.app.label.lowercase(Locale.ROOT) }
        .thenBy { result -> result.app.packageName }

    private fun InstalledApp.rankingScore(
        usage: Map<String, AppUsage>,
        pinnedPackages: Set<String>,
    ): Int {
        val pinnedScore = if (packageName in pinnedPackages) PINNED_SCORE else 0
        val launchCount = usage[packageName]?.launchCount ?: 0
        return pinnedScore + launchCount.coerceAtMost(MAX_USAGE_SCORE)
    }

    /** Outweighs any launch count, so a pinned application leads its match strength. */
    private const val PINNED_SCORE = 50

    /** Caps the launch reward, so a long-forgotten favourite cannot own the list forever. */
    private const val MAX_USAGE_SCORE = 20
}
