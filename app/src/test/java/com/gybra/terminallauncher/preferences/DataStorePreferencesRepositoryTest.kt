package com.gybra.terminallauncher.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gybra.terminallauncher.shell.ShellType
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DataStorePreferencesRepositoryTest {
    @Test
    fun `uses documented defaults when preferences are empty`() = runTest {
        val repository = DataStorePreferencesRepository(FakePreferencesDataStore())

        assertEquals(LauncherPreferences(), repository.preferences.first())
    }

    @Test
    fun `persists every launcher preference`() = runTest {
        val repository = DataStorePreferencesRepository(FakePreferencesDataStore())

        repository.setShellType(ShellType.DOS)
        repository.setShowClock(false)
        repository.setUsername("oreste")
        repository.setHostname("phone")
        repository.pinPackage("org.example.mail")

        assertEquals(
            LauncherPreferences(
                shellType = ShellType.DOS,
                showClock = false,
                username = "oreste",
                hostname = "phone",
                pinnedPackages = setOf("org.example.mail"),
            ),
            repository.preferences.first(),
        )
    }

    @Test
    fun `pin and unpin operations are idempotent`() = runTest {
        val repository = DataStorePreferencesRepository(FakePreferencesDataStore())

        repository.pinPackage("org.example.mail")
        repository.pinPackage("org.example.mail")
        repository.unpinPackage("org.example.missing")
        repository.unpinPackage("org.example.mail")
        repository.unpinPackage("org.example.mail")

        assertEquals(emptySet<String>(), repository.preferences.first().pinnedPackages)
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
