package com.gybra.terminallauncher.preferences

import com.gybra.terminallauncher.shell.ShellType
import com.gybra.terminallauncher.theme.TerminalTheme
import kotlinx.coroutines.flow.Flow

public interface PreferencesRepository {
    public val preferences: Flow<LauncherPreferences>

    public suspend fun setShellType(shellType: ShellType)

    public suspend fun setTerminalTheme(terminalTheme: TerminalTheme)

    public suspend fun setShowClock(showClock: Boolean)

    public suspend fun setUsername(username: String)

    public suspend fun setHostname(hostname: String)

    public suspend fun pinPackage(packageName: String)

    public suspend fun unpinPackage(packageName: String)

    /** Names [packageName] so the prompt launches it when [name] is submitted. */
    public suspend fun setAlias(name: String, packageName: String)
}
