package com.gybra.terminallauncher.preferences

import com.gybra.terminallauncher.launcher.AppUsage
import com.gybra.terminallauncher.launcher.PinnedShortcut
import com.gybra.terminallauncher.shell.DosDrive
import com.gybra.terminallauncher.shell.PromptSymbol
import com.gybra.terminallauncher.shell.ShellType
import com.gybra.terminallauncher.theme.TerminalTheme

public data class LauncherPreferences(
    public val shellType: ShellType = ShellType.UNIX,
    public val terminalTheme: TerminalTheme = TerminalTheme.SYSTEM,
    public val showClock: Boolean = true,
    public val showBattery: Boolean = true,
    public val username: String = "user",
    public val hostname: String = "android",
    public val promptSymbol: PromptSymbol = PromptSymbol.DOLLAR,
    public val showPromptPath: Boolean = true,
    public val dosDrive: DosDrive = DosDrive.C,
    public val pinnedPackages: Set<String> = emptySet(),
    public val pinnedShortcuts: List<PinnedShortcut> = emptyList(),
    public val aliases: Map<String, String> = emptyMap(),
    public val usage: Map<String, AppUsage> = emptyMap(),
)
