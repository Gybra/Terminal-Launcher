package com.gybra.terminallauncher.ui.home

import com.gybra.terminallauncher.launcher.InstalledApp

public data class HomeUiState(
    public val apps: List<InstalledApp> = emptyList(),
)
