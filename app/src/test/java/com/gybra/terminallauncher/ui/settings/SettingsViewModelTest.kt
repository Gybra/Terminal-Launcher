package com.gybra.terminallauncher.ui.settings

import com.gybra.terminallauncher.MainDispatcherRule
import com.gybra.terminallauncher.preferences.LauncherPreferences
import com.gybra.terminallauncher.preferences.PreferencesRepository
import com.gybra.terminallauncher.shell.ShellType
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

    private class FakePreferencesRepository : PreferencesRepository {
        private val mutablePreferences = MutableStateFlow(LauncherPreferences())
        override val preferences: Flow<LauncherPreferences> = mutablePreferences

        var shellType: ShellType? = null
        var showClock: Boolean? = null
        var username: String? = null
        var hostname: String? = null

        fun emit(preferences: LauncherPreferences) {
            mutablePreferences.value = preferences
        }

        override suspend fun setShellType(shellType: ShellType) {
            this.shellType = shellType
        }

        override suspend fun setShowClock(showClock: Boolean) {
            this.showClock = showClock
        }

        override suspend fun setUsername(username: String) {
            this.username = username
        }

        override suspend fun setHostname(hostname: String) {
            this.hostname = hostname
        }

        override suspend fun pinPackage(packageName: String) = unsupported()

        override suspend fun unpinPackage(packageName: String) = unsupported()

        private fun unsupported(): Nothing = error("Not required by this test")
    }
}
