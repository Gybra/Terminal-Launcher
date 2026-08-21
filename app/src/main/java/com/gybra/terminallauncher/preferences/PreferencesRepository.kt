package com.gybra.terminallauncher.preferences

import com.gybra.terminallauncher.launcher.AppShortcut
import com.gybra.terminallauncher.shell.DosDrive
import com.gybra.terminallauncher.shell.PromptSymbol
import com.gybra.terminallauncher.shell.ShellType
import com.gybra.terminallauncher.theme.TerminalTheme
import kotlinx.coroutines.flow.Flow

public interface PreferencesRepository {
    public val preferences: Flow<LauncherPreferences>

    public suspend fun setShellType(shellType: ShellType)

    public suspend fun setTerminalTheme(terminalTheme: TerminalTheme)

    public suspend fun setShowClock(showClock: Boolean)

    public suspend fun setShowBattery(showBattery: Boolean)

    /** Keeps the system bars hidden, or lets Android show them, as [immersiveMode] asks. */
    public suspend fun setImmersiveMode(immersiveMode: Boolean)

    public suspend fun setUsername(username: String)

    public suspend fun setHostname(hostname: String)

    public suspend fun setPromptSymbol(promptSymbol: PromptSymbol)

    public suspend fun setShowPromptPath(showPromptPath: Boolean)

    public suspend fun setDosDrive(dosDrive: DosDrive)

    public suspend fun pinPackage(packageName: String)

    public suspend fun unpinPackage(packageName: String)

    /** Keeps [shortcut] on Home, replacing what was stored for the same shortcut. */
    public suspend fun pinShortcut(shortcut: AppShortcut)

    /** Removes [shortcut] from Home, leaving every other shortcut of its application pinned. */
    public suspend fun unpinShortcut(shortcut: AppShortcut)

    /** Removes every shortcut of [packageName] from Home, such as after an uninstall. */
    public suspend fun unpinShortcuts(packageName: String)

    /** Names [packageName] so the prompt launches it when [name] is submitted. */
    public suspend fun setAlias(name: String, packageName: String)

    /** Counts one launch of [packageName] at [launchedAt], so search can rank it higher. */
    public suspend fun recordLaunch(packageName: String, launchedAt: Long)
}
