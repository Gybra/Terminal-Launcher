package com.gybra.terminallauncher.ui.settings

import com.gybra.terminallauncher.MainDispatcherRule
import com.gybra.terminallauncher.preferences.LauncherPreferences
import com.gybra.terminallauncher.preferences.PreferencesRepository
import com.gybra.terminallauncher.shell.ShellType
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `exposes preferences and reacts to repository updates`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakePreferencesRepository()
        val viewModel = SettingsViewModel(repository)
        advanceUntilIdle()

        repository.emit(
            LauncherPreferences(
                shellType = ShellType.DOS,
                showClock = false,
                username = "oreste",
                hostname = "phone",
            ),
        )
        advanceUntilIdle()

        assertEquals(
            SettingsUiState(
                shellType = ShellType.DOS,
                showClock = false,
                username = "oreste",
                hostname = "phone",
            ),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `delegates every setting change to the repository`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakePreferencesRepository()
        val viewModel = SettingsViewModel(repository)

        viewModel.selectShell(ShellType.DOS)
        viewModel.setShowClock(false)
        viewModel.setUsername("oreste")
        viewModel.setHostname("phone")
        advanceUntilIdle()

        assertEquals(ShellType.DOS, repository.shellType)
        assertEquals(false, repository.showClock)
        assertEquals("oreste", repository.username)
        assertEquals("phone", repository.hostname)
    }

    @Test
    fun `keeps rapid controlled input updates in order`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakePreferencesRepository()
        val viewModel = SettingsViewModel(repository)

        viewModel.setUsername("o")
        viewModel.setUsername("or")
        viewModel.setUsername("oreste")

        assertEquals("oreste", viewModel.uiState.value.username)

        advanceUntilIdle()

        assertEquals(listOf("o", "or", "oreste"), repository.usernameWrites)
        assertEquals("oreste", viewModel.uiState.value.username)
    }

    @Test
    fun `recovers from storage write failures without crashing`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakePreferencesRepository(writeFailure = IOException("disk full"))
        val viewModel = SettingsViewModel(repository)

        viewModel.setShowClock(false)
        assertEquals(false, viewModel.uiState.value.showClock)
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.showClock)
        assertEquals("Unable to save preferences", viewModel.uiState.value.storageError)
    }

    private class FakePreferencesRepository(
        private val writeFailure: IOException? = null,
    ) : PreferencesRepository {
        private val mutablePreferences = MutableStateFlow(LauncherPreferences())
        override val preferences: Flow<LauncherPreferences> = mutablePreferences

        var shellType: ShellType? = null
        var showClock: Boolean? = null
        var username: String? = null
        var hostname: String? = null
        val usernameWrites = mutableListOf<String>()

        fun emit(preferences: LauncherPreferences) {
            mutablePreferences.value = preferences
        }

        override suspend fun setShellType(shellType: ShellType) {
            writeFailure?.let { throw it }
            this.shellType = shellType
            emit(mutablePreferences.value.copy(shellType = shellType))
        }

        override suspend fun setShowClock(showClock: Boolean) {
            writeFailure?.let { throw it }
            this.showClock = showClock
            emit(mutablePreferences.value.copy(showClock = showClock))
        }

        override suspend fun setUsername(username: String) {
            writeFailure?.let { throw it }
            this.username = username
            usernameWrites += username
            emit(mutablePreferences.value.copy(username = username))
        }

        override suspend fun setHostname(hostname: String) {
            writeFailure?.let { throw it }
            this.hostname = hostname
            emit(mutablePreferences.value.copy(hostname = hostname))
        }

        override suspend fun pinPackage(packageName: String) = unsupported()

        override suspend fun unpinPackage(packageName: String) = unsupported()

        private fun unsupported(): Nothing = error("Not required by this test")
    }
}
