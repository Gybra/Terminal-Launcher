package com.gybra.terminallauncher.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.gybra.terminallauncher.shell.ShellType
import com.gybra.terminallauncher.theme.TerminalTheme
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

public class DataStorePreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) : PreferencesRepository {
    private val defaults = LauncherPreferences()

    override val preferences: Flow<LauncherPreferences> = dataStore.data
        .catch { failure ->
            if (failure is IOException) {
                emit(emptyPreferences())
            } else {
                throw failure
            }
        }
        .map(::mapPreferences)

    override suspend fun setShellType(shellType: ShellType) {
        dataStore.edit { preferences ->
            preferences[Keys.shellType] = shellType.name
        }
    }

    override suspend fun setTerminalTheme(terminalTheme: TerminalTheme) {
        dataStore.edit { preferences ->
            preferences[Keys.terminalTheme] = terminalTheme.name
        }
    }

    override suspend fun setShowClock(showClock: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.showClock] = showClock
        }
    }

    override suspend fun setUsername(username: String) {
        dataStore.edit { preferences ->
            preferences[Keys.username] = username
        }
    }

    override suspend fun setHostname(hostname: String) {
        dataStore.edit { preferences ->
            preferences[Keys.hostname] = hostname
        }
    }

    override suspend fun pinPackage(packageName: String) {
        require(packageName.isNotBlank()) { "Package name must not be blank" }
        dataStore.edit { preferences ->
            preferences[Keys.pinnedPackages] =
                (preferences[Keys.pinnedPackages] ?: defaults.pinnedPackages) + packageName
        }
    }

    override suspend fun unpinPackage(packageName: String) {
        require(packageName.isNotBlank()) { "Package name must not be blank" }
        dataStore.edit { preferences ->
            preferences[Keys.pinnedPackages] =
                (preferences[Keys.pinnedPackages] ?: defaults.pinnedPackages) - packageName
        }
    }

    override suspend fun setAlias(name: String, packageName: String) {
        require(name.isNotBlank()) { "Alias name must not be blank" }
        require(packageName.isNotBlank()) { "Package name must not be blank" }
        dataStore.edit { preferences ->
            val stored = preferences[Keys.aliases] ?: emptySet()
            preferences[Keys.aliases] =
                stored.filterNot { entry -> entry.aliasName() == name }.toSet() +
                    "$name$ALIAS_SEPARATOR$packageName"
        }
    }

    private fun mapPreferences(preferences: Preferences): LauncherPreferences = LauncherPreferences(
        shellType = preferences[Keys.shellType].toShellType(),
        terminalTheme = preferences[Keys.terminalTheme].toTerminalTheme(),
        showClock = preferences[Keys.showClock] ?: defaults.showClock,
        username = preferences[Keys.username] ?: defaults.username,
        hostname = preferences[Keys.hostname] ?: defaults.hostname,
        pinnedPackages = preferences[Keys.pinnedPackages] ?: defaults.pinnedPackages,
        aliases = preferences[Keys.aliases]?.toAliases() ?: defaults.aliases,
    )

    /**
     * Reads the stored `name=package` entries, dropping anything malformed, and keeps them ordered
     * by name so listings and tests stay deterministic.
     */
    private fun Set<String>.toAliases(): Map<String, String> = sorted()
        .mapNotNull { entry ->
            val name = entry.aliasName()
            val packageName = entry.substringAfterLast(ALIAS_SEPARATOR, missingDelimiterValue = "")
            if (name.isBlank() || packageName.isBlank()) null else name to packageName
        }
        .toMap()

    private fun String.aliasName(): String =
        substringBeforeLast(ALIAS_SEPARATOR, missingDelimiterValue = "")

    private fun String?.toShellType(): ShellType =
        ShellType.entries.firstOrNull { shellType -> shellType.name == this } ?: defaults.shellType

    private fun String?.toTerminalTheme(): TerminalTheme =
        TerminalTheme.entries.firstOrNull { theme -> theme.name == this } ?: defaults.terminalTheme

    private object Keys {
        val shellType: Preferences.Key<String> = stringPreferencesKey("shell_type")
        val terminalTheme: Preferences.Key<String> = stringPreferencesKey("terminal_theme")
        val showClock: Preferences.Key<Boolean> = booleanPreferencesKey("show_clock")
        val username: Preferences.Key<String> = stringPreferencesKey("username")
        val hostname: Preferences.Key<String> = stringPreferencesKey("hostname")
        val pinnedPackages: Preferences.Key<Set<String>> = stringSetPreferencesKey("pinned_packages")
        val aliases: Preferences.Key<Set<String>> = stringSetPreferencesKey("aliases")
    }

    private companion object {
        /**
         * Separates an alias from its package. A package name never contains it, so reading the
         * last one back apart keeps names that do.
         */
        const val ALIAS_SEPARATOR = '='
    }
}
