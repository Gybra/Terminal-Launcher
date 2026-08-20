package com.gybra.terminallauncher.ui.home

/**
 * One submitted line of the terminal history: what the user typed and what it printed. The
 * identifier only keeps rendering stable while older entries are dropped; it is never persisted.
 */
public data class TerminalEntry(
    public val id: Long,
    public val input: String,
    public val output: List<String>,
)
