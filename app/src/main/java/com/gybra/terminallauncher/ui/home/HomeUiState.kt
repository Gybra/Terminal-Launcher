package com.gybra.terminallauncher.ui.home

import com.gybra.terminallauncher.launcher.InstalledApp

data class HomeUiState(
    val apps: List<InstalledApp> = emptyList(),
)

