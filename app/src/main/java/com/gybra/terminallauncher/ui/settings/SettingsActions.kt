package com.gybra.terminallauncher.ui.settings

import com.gybra.terminallauncher.shell.ShellType
import com.gybra.terminallauncher.theme.TerminalTheme

public data class SettingsActions(
    public val selectShell: (ShellType) -> Unit,
    public val selectTheme: (TerminalTheme) -> Unit,
    public val setShowClock: (Boolean) -> Unit,
    public val setUsername: (String) -> Unit,
    public val setHostname: (String) -> Unit,
)
