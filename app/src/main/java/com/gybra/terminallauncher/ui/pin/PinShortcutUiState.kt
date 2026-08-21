package com.gybra.terminallauncher.ui.pin

import com.gybra.terminallauncher.launcher.AppShortcut
import com.gybra.terminallauncher.shell.ShellProfile
import com.gybra.terminallauncher.theme.TerminalTheme

/** What the confirmation shows while an application waits for an answer about [shortcut]. */
public data class PinShortcutUiState(
    public val shortcut: AppShortcut,
    public val shellProfile: ShellProfile,
    public val terminalTheme: TerminalTheme,
)
