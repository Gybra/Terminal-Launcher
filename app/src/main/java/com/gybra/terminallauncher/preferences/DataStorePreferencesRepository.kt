package com.gybra.terminallauncher.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.gybra.terminallauncher.shell.ShellType
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

    private fun mapPreferences(preferences: Preferences): LauncherPreferences = LauncherPreferences(
        shellType = preferences[Keys.shellType].toShellType(),
        showClock = preferences[Keys.showClock] ?: defaults.showClock,
        username = preferences[Keys.username] ?: defaults.username,
        hostname = preferences[Keys.hostname] ?: defaults.hostname,
        pinnedPackages = preferences[Keys.pinnedPackages] ?: defaults.pinnedPackages,
    )

    private fun String?.toShellType(): ShellType =
        ShellType.entries.firstOrNull { shellType -> shellType.name == this } ?: defaults.shellType

    private object Keys {
        val shellType: Preferences.Key<String> = stringPreferencesKey("shell_type")
        val showClock: Preferences.Key<Boolean> = booleanPreferencesKey("show_clock")
        val username: Preferences.Key<String> = stringPreferencesKey("username")
        val hostname: Preferences.Key<String> = stringPreferencesKey("hostname")
        val pinnedPackages: Preferences.Key<Set<String>> = stringSetPreferencesKey("pinned_packages")
    }
}
