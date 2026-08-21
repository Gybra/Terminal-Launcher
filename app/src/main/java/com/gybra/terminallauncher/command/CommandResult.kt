package com.gybra.terminallauncher.command

import com.gybra.terminallauncher.launcher.AppShortcut
import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.launcher.SystemScreen

/** What the launcher does with submitted prompt input. */
public sealed interface CommandResult {
    /** A registered command ran and produced [lines], which are empty when it only changed state. */
    public data class Output(public val lines: List<String>) : CommandResult

    /**
     * A registered command listed [apps] or [shortcuts] under [lines]. The launcher writes the
     * lines and keeps what was listed as rows, so the one that is tapped starts. Both lists are
     * empty when a name resolved to nothing, so an unresolved name is answered the same way
     * whether or not it left candidates behind.
     */
    public data class Listing(
        public val lines: List<String>,
        public val apps: List<InstalledApp> = emptyList(),
        public val shortcuts: List<AppShortcut> = emptyList(),
    ) : CommandResult

    /** A registered command asked the launcher to erase the terminal history. */
    public object ClearHistory : CommandResult

    /** A registered command asked the launcher to open the settings destination. */
    public object OpenSettings : CommandResult

    /** A registered command asked Android to open [screen]. */
    public data class OpenSystemScreen(public val screen: SystemScreen) : CommandResult

    /** A registered command asked the launcher to start its Home destination again. */
    public object RestartLauncher : CommandResult

    /** No registered command matched, so the input is searched among installed applications. */
    public object Search : CommandResult
}
