package com.gybra.terminallauncher.ui.settings

import com.gybra.terminallauncher.shell.ShellType
import com.gybra.terminallauncher.theme.TerminalTheme

public data class SettingsUiState(
    public val shellType: ShellType,
    public val terminalTheme: TerminalTheme,
    public val showClock: Boolean,
    public val username: String,
    public val hostname: String,
    public val storageError: String? = null,
)
