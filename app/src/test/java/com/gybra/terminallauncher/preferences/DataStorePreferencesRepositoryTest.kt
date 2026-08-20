package com.gybra.terminallauncher.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.gybra.terminallauncher.launcher.AppUsage
import com.gybra.terminallauncher.shell.ShellType
import com.gybra.terminallauncher.theme.TerminalTheme
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStorePreferencesRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `uses documented defaults when preferences are empty`() = runTest {
        val repository = DataStorePreferencesRepository(FakePreferencesDataStore())

        assertEquals(LauncherPreferences(), repository.preferences.first())
    }

    @Test
    fun `persists every launcher preference`() = runTest {
        val storageFile = File(temporaryFolder.root, "launcher.preferences_pb")
        val firstJob = SupervisorJob()
        val firstRepository = fileBackedRepository(
            storageFile = storageFile,
            scope = CoroutineScope(firstJob + Dispatchers.IO),
        )

        firstRepository.setShellType(ShellType.DOS)
        firstRepository.setTerminalTheme(TerminalTheme.AMBER)
        firstRepository.setShowClock(false)
        firstRepository.setUsername("oreste")
        firstRepository.setHostname("phone")
        firstRepository.pinPackage("org.example.mail")
        firstRepository.setAlias(name = "browser", packageName = "org.example.firefox")
        firstRepository.recordLaunch(packageName = "org.example.mail", launchedAt = LAUNCHED_AT)
        firstJob.cancelAndJoin()

        val secondJob = SupervisorJob()
        val restoredPreferences = fileBackedRepository(
            storageFile = storageFile,
            scope = CoroutineScope(secondJob + Dispatchers.IO),
        ).preferences.first()
        secondJob.cancelAndJoin()

        assertEquals(
            LauncherPreferences(
                shellType = ShellType.DOS,
                terminalTheme = TerminalTheme.AMBER,
                showClock = false,
                username = "oreste",
                hostname = "phone",
                pinnedPackages = setOf("org.example.mail"),
                aliases = mapOf("browser" to "org.example.firefox"),
                usage = mapOf(
                    "org.example.mail" to AppUsage(launchCount = 1, lastLaunchedAt = LAUNCHED_AT),
                ),
            ),
            restoredPreferences,
        )
    }

    @Test
    fun `keeps one package per alias name and orders aliases by name`() = runTest {
        val repository = DataStorePreferencesRepository(FakePreferencesDataStore())

        repository.setAlias(name = "browser", packageName = "org.example.firefox")
        repository.setAlias(name = "mail", packageName = "org.example.mail")
        repository.setAlias(name = "browser", packageName = "org.example.chrome")

        assertEquals(
            listOf("browser" to "org.example.chrome", "mail" to "org.example.mail"),
            repository.preferences.first().aliases.toList(),
        )
    }

    @Test
    fun `keeps alias names that contain the stored separator`() = runTest {
        val repository = DataStorePreferencesRepository(FakePreferencesDataStore())

        repository.setAlias(name = "a=b", packageName = "org.example.firefox")

        assertEquals(
            mapOf("a=b" to "org.example.firefox"),
            repository.preferences.first().aliases,
        )
    }

    @Test
    fun `ignores stored aliases that are not readable`() = runTest {
        val storedPreferences = mutablePreferencesOf(
            stringSetPreferencesKey("aliases") to setOf(
                "browser=org.example.firefox",
                "missing-separator",
                "=org.example.orphan",
                "orphan=",
            ),
        )

        val aliases = DataStorePreferencesRepository(FakePreferencesDataStore(storedPreferences))
            .preferences
            .first()
            .aliases

        assertEquals(mapOf("browser" to "org.example.firefox"), aliases)
    }

    @Test
    fun `refuses to store a blank alias`() {
        val repository = DataStorePreferencesRepository(FakePreferencesDataStore())

        assertThrows(IllegalArgumentException::class.java) {
            runTest { repository.setAlias(name = "  ", packageName = "org.example.firefox") }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runTest { repository.setAlias(name = "browser", packageName = "  ") }
        }
    }

    @Test
    fun `counts every launch and keeps the last launch time`() = runTest {
        val repository = DataStorePreferencesRepository(FakePreferencesDataStore())

        repository.recordLaunch(packageName = "org.example.mail", launchedAt = LAUNCHED_AT)
        repository.recordLaunch(packageName = "org.example.browser", launchedAt = LAUNCHED_AT)
        repository.recordLaunch(packageName = "org.example.mail", launchedAt = RELAUNCHED_AT)

        assertEquals(
            mapOf(
                "org.example.mail" to AppUsage(launchCount = 2, lastLaunchedAt = RELAUNCHED_AT),
                "org.example.browser" to AppUsage(launchCount = 1, lastLaunchedAt = LAUNCHED_AT),
            ),
            repository.preferences.first().usage,
        )
    }

    @Test
    fun `ignores stored application usage that is not readable`() = runTest {
        val storedPreferences = mutablePreferencesOf(
            stringSetPreferencesKey("app_usage") to setOf(
                "org.example.mail=2=1700000000000",
                "org.example.missing-fields",
                "org.example.browser=often=1700000000000",
                "org.example.clock=2=recently",
                "=2=1700000000000",
            ),
        )

        val usage = DataStorePreferencesRepository(FakePreferencesDataStore(storedPreferences))
            .preferences
            .first()
            .usage

        assertEquals(
            mapOf("org.example.mail" to AppUsage(launchCount = 2, lastLaunchedAt = LAUNCHED_AT)),
            usage,
        )
    }

    @Test
    fun `pin and unpin operations are idempotent`() = runTest {
        val repository = DataStorePreferencesRepository(FakePreferencesDataStore())

        repository.unpinPackage("org.example.missing")
        assertEquals(emptySet<String>(), repository.preferences.first().pinnedPackages)

        repository.pinPackage("org.example.mail")
        repository.pinPackage("org.example.mail")
        repository.pinPackage("org.example.browser")
        repository.unpinPackage("org.example.missing")

        assertEquals(
            setOf("org.example.mail", "org.example.browser"),
            repository.preferences.first().pinnedPackages,
        )

        repository.unpinPackage("org.example.mail")
        repository.unpinPackage("org.example.mail")

        assertEquals(
            setOf("org.example.browser"),
            repository.preferences.first().pinnedPackages,
        )
    }

    @Test
    fun `rejects blank package names`() {
        val repository = DataStorePreferencesRepository(FakePreferencesDataStore())

        assertThrows(IllegalArgumentException::class.java) {
            runTest { repository.pinPackage(" ") }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runTest { repository.unpinPackage("") }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runTest { repository.recordLaunch(packageName = " ", launchedAt = LAUNCHED_AT) }
        }
    }

    @Test
    fun `falls back to defaults after a storage read failure`() = runTest {
        val repository = DataStorePreferencesRepository(
            FakePreferencesDataStore(readFailure = IOException("disk unavailable")),
        )

        assertEquals(LauncherPreferences(), repository.preferences.first())
    }

    @Test
    fun `propagates unexpected storage failures`() {
        val repository = DataStorePreferencesRepository(
            FakePreferencesDataStore(readFailure = IllegalStateException("broken contract")),
        )

        assertThrows(IllegalStateException::class.java) {
            runTest { repository.preferences.first() }
        }
    }

    @Test
    fun `maps an unknown persisted shell to the default shell`() = runTest {
        val storedPreferences = mutablePreferencesOf(
            stringPreferencesKey("shell_type") to "UNKNOWN",
        )
        val repository = DataStorePreferencesRepository(
            FakePreferencesDataStore(initialPreferences = storedPreferences),
        )

        assertEquals(ShellType.UNIX, repository.preferences.first().shellType)
    }

    @Test
    fun `maps an unknown persisted theme to the default theme`() = runTest {
        val storedPreferences = mutablePreferencesOf(
            stringPreferencesKey("terminal_theme") to "UNKNOWN",
        )
        val repository = DataStorePreferencesRepository(
            FakePreferencesDataStore(initialPreferences = storedPreferences),
        )

        assertEquals(TerminalTheme.SYSTEM, repository.preferences.first().terminalTheme)
    }

    private companion object {
        const val LAUNCHED_AT = 1_700_000_000_000L
        const val RELAUNCHED_AT = LAUNCHED_AT + 60_000L
    }

    private fun fileBackedRepository(
        storageFile: File,
        scope: CoroutineScope,
    ): DataStorePreferencesRepository = DataStorePreferencesRepository(
        dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { storageFile },
        ),
    )

    private class FakePreferencesDataStore(
        initialPreferences: Preferences = emptyPreferences(),
        readFailure: Throwable? = null,
    ) : DataStore<Preferences> {
        private val storedPreferences = MutableStateFlow(initialPreferences)

        override val data: Flow<Preferences> = readFailure?.let { failure ->
            flow { throw failure }
        } ?: storedPreferences

        override suspend fun updateData(
            transform: suspend (preferences: Preferences) -> Preferences,
        ): Preferences {
            val updatedPreferences = transform(storedPreferences.value)
            storedPreferences.value = updatedPreferences
            return updatedPreferences
        }
    }
}
