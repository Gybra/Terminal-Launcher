package com.gybra.terminallauncher.preferences

import com.gybra.terminallauncher.shell.ShellType
import com.gybra.terminallauncher.theme.TerminalTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** A [PreferencesRepository] that keeps preferences in memory and records every write. */
class RecordingPreferencesRepository(
    initialPreferences: LauncherPreferences = LauncherPreferences(),
) : PreferencesRepository {
    private val storedPreferences = MutableStateFlow(initialPreferences)

    override val preferences: Flow<LauncherPreferences> = storedPreferences

    val writes: MutableList<String> = mutableListOf()

    fun emit(preferences: LauncherPreferences) {
        storedPreferences.value = preferences
    }

    override suspend fun setShellType(shellType: ShellType) {
        writes += "setShellType($shellType)"
        storedPreferences.update { preferences -> preferences.copy(shellType = shellType) }
    }

    override suspend fun setTerminalTheme(terminalTheme: TerminalTheme) {
        writes += "setTerminalTheme($terminalTheme)"
        storedPreferences.update { preferences -> preferences.copy(terminalTheme = terminalTheme) }
    }

    override suspend fun setShowClock(showClock: Boolean) {
        writes += "setShowClock($showClock)"
        storedPreferences.update { preferences -> preferences.copy(showClock = showClock) }
    }

    override suspend fun setUsername(username: String) {
        writes += "setUsername($username)"
        storedPreferences.update { preferences -> preferences.copy(username = username) }
    }

    override suspend fun setHostname(hostname: String) {
        writes += "setHostname($hostname)"
        storedPreferences.update { preferences -> preferences.copy(hostname = hostname) }
    }

    override suspend fun pinPackage(packageName: String) {
        writes += "pinPackage($packageName)"
        storedPreferences.update { preferences ->
            preferences.copy(pinnedPackages = preferences.pinnedPackages + packageName)
        }
    }

    override suspend fun unpinPackage(packageName: String) {
        writes += "unpinPackage($packageName)"
        storedPreferences.update { preferences ->
            preferences.copy(pinnedPackages = preferences.pinnedPackages - packageName)
        }
    }
}
