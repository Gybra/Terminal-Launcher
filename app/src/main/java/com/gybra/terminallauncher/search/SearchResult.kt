package com.gybra.terminallauncher.search

import com.gybra.terminallauncher.launcher.InstalledApp

/**
 * An application matched by [AppSearchEngine], together with the strength of the match that
 * determines its rank among the other results.
 */
public data class SearchResult(
    public val app: InstalledApp,
    public val match: Match,
) {
    /** Match strengths, declared from the strongest to the weakest. */
    public enum class Match {
        EXACT,
        PREFIX,
        SUBSTRING,
        FUZZY,
    }
}
