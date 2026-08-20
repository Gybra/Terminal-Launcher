package com.gybra.terminallauncher.preferences

import com.gybra.terminallauncher.launcher.AppUsage
import com.gybra.terminallauncher.shell.DosDrive
import com.gybra.terminallauncher.shell.PromptSymbol
import com.gybra.terminallauncher.shell.ShellType
import com.gybra.terminallauncher.theme.TerminalTheme
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * A [PreferencesRepository] that keeps preferences in memory, records every write, and fails them
 * all when [writeFailure] is given.
 */
class RecordingPreferencesRepository(
    initialPreferences: LauncherPreferences = LauncherPreferences(),
    private val writeFailure: IOException? = null,
) : PreferencesRepository {
    private val storedPreferences = MutableStateFlow(initialPreferences)

    override val preferences: Flow<LauncherPreferences> = storedPreferences

    val writes: MutableList<String> = mutableListOf()

    fun emit(preferences: LauncherPreferences) {
        storedPreferences.value = preferences
    }

    override suspend fun setShellType(shellType: ShellType) {
        write("setShellType($shellType)") { preferences -> preferences.copy(shellType = shellType) }
    }

    override suspend fun setTerminalTheme(terminalTheme: TerminalTheme) {
        write("setTerminalTheme($terminalTheme)") { preferences ->
            preferences.copy(terminalTheme = terminalTheme)
        }
    }

    override suspend fun setShowClock(showClock: Boolean) {
        write("setShowClock($showClock)") { preferences -> preferences.copy(showClock = showClock) }
    }

    override suspend fun setUsername(username: String) {
        write("setUsername($username)") { preferences -> preferences.copy(username = username) }
    }

    override suspend fun setHostname(hostname: String) {
        write("setHostname($hostname)") { preferences -> preferences.copy(hostname = hostname) }
    }

    override suspend fun setPromptSymbol(promptSymbol: PromptSymbol) {
        write("setPromptSymbol($promptSymbol)") { preferences ->
            preferences.copy(promptSymbol = promptSymbol)
        }
    }

    override suspend fun setShowPromptPath(showPromptPath: Boolean) {
        write("setShowPromptPath($showPromptPath)") { preferences ->
            preferences.copy(showPromptPath = showPromptPath)
        }
    }

    override suspend fun setDosDrive(dosDrive: DosDrive) {
        write("setDosDrive($dosDrive)") { preferences ->
            preferences.copy(dosDrive = dosDrive)
        }
    }

    override suspend fun pinPackage(packageName: String) {
        write("pinPackage($packageName)") { preferences ->
            preferences.copy(pinnedPackages = preferences.pinnedPackages + packageName)
        }
    }

    override suspend fun unpinPackage(packageName: String) {
        write("unpinPackage($packageName)") { preferences ->
            preferences.copy(pinnedPackages = preferences.pinnedPackages - packageName)
        }
    }

    override suspend fun setAlias(name: String, packageName: String) {
        write("setAlias($name, $packageName)") { preferences ->
            preferences.copy(aliases = preferences.aliases + (name to packageName))
        }
    }

    override suspend fun recordLaunch(packageName: String, launchedAt: Long) {
        write("recordLaunch($packageName, $launchedAt)") { preferences ->
            val launched = AppUsage(
                launchCount = (preferences.usage[packageName]?.launchCount ?: 0) + 1,
                lastLaunchedAt = launchedAt,
            )
            preferences.copy(usage = preferences.usage + (packageName to launched))
        }
    }

    private fun write(call: String, update: (LauncherPreferences) -> LauncherPreferences) {
        writes += call
        writeFailure?.let { failure -> throw failure }
        storedPreferences.update(update)
    }
}
