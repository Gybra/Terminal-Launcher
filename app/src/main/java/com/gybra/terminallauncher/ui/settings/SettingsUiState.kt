package com.gybra.terminallauncher.ui.settings

import com.gybra.terminallauncher.shell.ShellType

public data class SettingsUiState(
    public val shellType: ShellType,
    public val showClock: Boolean,
    public val username: String,
    public val hostname: String,
)
