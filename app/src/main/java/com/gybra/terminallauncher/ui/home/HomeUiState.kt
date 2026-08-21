package com.gybra.terminallauncher.ui.home

import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.launcher.AppShortcut
import com.gybra.terminallauncher.search.SearchResult
import com.gybra.terminallauncher.shell.ShellContext
import com.gybra.terminallauncher.shell.ShellProfile

public data class HomeUiState(
    public val shellProfile: ShellProfile,
    public val shellContext: ShellContext,
    public val apps: List<InstalledApp> = emptyList(),
    public val shortcuts: List<AppShortcut> = emptyList(),
    public val searchResults: List<SearchResult> = emptyList(),
    public val history: List<TerminalEntry> = emptyList(),
    public val statusClock: String? = null,
    public val statusBattery: String? = null,
    public val prompt: PromptState = PromptState(),
)
