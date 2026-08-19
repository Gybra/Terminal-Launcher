package com.gybra.terminallauncher.ui.home

import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.shell.ShellProfile

public data class HomeUiState(
    public val apps: List<InstalledApp> = emptyList(),
    public val shellProfile: ShellProfile,
)
