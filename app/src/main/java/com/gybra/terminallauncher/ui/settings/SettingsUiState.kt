package com.gybra.terminallauncher.ui.settings

import com.gybra.terminallauncher.shell.DosDrive
import com.gybra.terminallauncher.shell.PromptSymbol
import com.gybra.terminallauncher.shell.ShellType
import com.gybra.terminallauncher.theme.TerminalTheme

public data class SettingsUiState(
    public val shellType: ShellType,
    public val terminalTheme: TerminalTheme,
    public val showClock: Boolean,
    public val showBattery: Boolean,
    public val immersiveMode: Boolean,
    public val doubleTapToLock: Boolean,
    public val username: String,
    public val hostname: String,
    public val promptSymbol: PromptSymbol,
    public val showPromptPath: Boolean,
    public val dosDrive: DosDrive,
    public val storageError: String? = null,
)
