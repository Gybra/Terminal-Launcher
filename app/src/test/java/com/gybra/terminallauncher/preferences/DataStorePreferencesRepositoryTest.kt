package com.gybra.terminallauncher.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gybra.terminallauncher.shell.ShellType
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
        firstRepository.setShowClock(false)
        firstRepository.setUsername("oreste")
        firstRepository.setHostname("phone")
        firstRepository.pinPackage("org.example.mail")
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
                showClock = false,
                username = "oreste",
                hostname = "phone",
                pinnedPackages = setOf("org.example.mail"),
            ),
            restoredPreferences,
        )
    }

    @Test
    fun `pin and unpin operations are idempotent`() = runTest {
        val repository = DataStorePreferencesRepository(FakePreferencesDataStore())

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
