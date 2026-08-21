package com.gybra.terminallauncher.ui.home

import com.gybra.terminallauncher.launcher.AppShortcut
import com.gybra.terminallauncher.launcher.InstalledApp

/**
 * One submitted line of the terminal history: what the user typed, what it printed, and the
 * applications and shortcuts it listed, which stay startable. The identifier only keeps rendering
 * stable while older entries are dropped; it is never persisted.
 */
public data class TerminalEntry(
    public val id: Long,
    public val input: String,
    public val output: List<String>,
    public val apps: List<InstalledApp> = emptyList(),
    public val shortcuts: List<AppShortcut> = emptyList(),
)
