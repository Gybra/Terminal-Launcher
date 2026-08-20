package com.gybra.terminallauncher.ui.settings

import com.gybra.terminallauncher.MainDispatcherRule
import com.gybra.terminallauncher.preferences.LauncherPreferences
import com.gybra.terminallauncher.preferences.PreferencesRepository
import com.gybra.terminallauncher.shell.DosDrive
import com.gybra.terminallauncher.shell.PromptSymbol
import com.gybra.terminallauncher.shell.ShellType
import com.gybra.terminallauncher.theme.TerminalTheme
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
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
                terminalTheme = TerminalTheme.GREEN,
                showClock = false,
                username = "oreste",
                hostname = "phone",
                promptSymbol = PromptSymbol.PERCENT,
                showPromptPath = false,
                dosDrive = DosDrive.D,
            ),
        )
        advanceUntilIdle()

        assertEquals(
            SettingsUiState(
                shellType = ShellType.DOS,
                terminalTheme = TerminalTheme.GREEN,
                showClock = false,
                username = "oreste",
                hostname = "phone",
                promptSymbol = PromptSymbol.PERCENT,
                showPromptPath = false,
                dosDrive = DosDrive.D,
            ),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `delegates every setting change to the repository`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakePreferencesRepository()
        val viewModel = SettingsViewModel(repository)

        viewModel.selectShell(ShellType.DOS)
        viewModel.selectTheme(TerminalTheme.AMBER)
        viewModel.setShowClock(false)
        viewModel.setUsername("oreste")
        viewModel.setHostname("phone")
        viewModel.selectPromptSymbol(PromptSymbol.ARROW)
        viewModel.setShowPromptPath(false)
        viewModel.selectDosDrive(DosDrive.A)
        advanceUntilIdle()

        assertEquals(ShellType.DOS, repository.shellType)
        assertEquals(TerminalTheme.AMBER, repository.terminalTheme)
        assertEquals(false, repository.showClock)
        assertEquals("oreste", repository.username)
        assertEquals("phone", repository.hostname)
        assertEquals(PromptSymbol.ARROW, repository.promptSymbol)
        assertEquals(false, repository.showPromptPath)
        assertEquals(DosDrive.A, repository.dosDrive)
    }

    @Test
    fun `keeps an identity usable in a prompt`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakePreferencesRepository()
        val viewModel = SettingsViewModel(repository)

        viewModel.setUsername(" or\teste\n")
        viewModel.setHostname("a very long hostname indeed")
        advanceUntilIdle()

        assertEquals("oreste", viewModel.uiState.value.username)
        assertEquals("averylonghostnam", viewModel.uiState.value.hostname)
        assertEquals("oreste", repository.username)
        assertEquals("averylonghostnam", repository.hostname)
    }

    @Test
    fun `clears an identity made only of unusable characters`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository = FakePreferencesRepository()
            val viewModel = SettingsViewModel(repository)

            viewModel.setUsername("  \t ")
            advanceUntilIdle()

            assertEquals("", viewModel.uiState.value.username)
            assertEquals("", repository.username)
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

    @Test
    fun `does not replace a newer edit when failure recovery resumes`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository = RefreshRacePreferencesRepository()
            val viewModel = SettingsViewModel(repository)
            runCurrent()

            viewModel.setShowClock(false)
            repository.refreshStarted.await()
            viewModel.setUsername("oreste")
            runCurrent()

            repository.releaseRefresh.complete(Unit)
            runCurrent()

            assertEquals("oreste", viewModel.uiState.value.username)

            repository.releaseUsernameWrite.complete(Unit)
            advanceUntilIdle()

            assertEquals("oreste", viewModel.uiState.value.username)
            assertEquals("Unable to save preferences", viewModel.uiState.value.storageError)
        }

    private class FakePreferencesRepository(
        private val writeFailure: IOException? = null,
    ) : PreferencesRepository {
        private val mutablePreferences = MutableStateFlow(LauncherPreferences())
        override val preferences: Flow<LauncherPreferences> = mutablePreferences

        var shellType: ShellType? = null
        var terminalTheme: TerminalTheme? = null
        var showClock: Boolean? = null
        var username: String? = null
        var hostname: String? = null
        var promptSymbol: PromptSymbol? = null
        var showPromptPath: Boolean? = null
        var dosDrive: DosDrive? = null
        val usernameWrites = mutableListOf<String>()

        fun emit(preferences: LauncherPreferences) {
            mutablePreferences.value = preferences
        }

        override suspend fun setShellType(shellType: ShellType) {
            writeFailure?.let { throw it }
            this.shellType = shellType
            emit(mutablePreferences.value.copy(shellType = shellType))
        }

        override suspend fun setTerminalTheme(terminalTheme: TerminalTheme) {
            writeFailure?.let { throw it }
            this.terminalTheme = terminalTheme
            emit(mutablePreferences.value.copy(terminalTheme = terminalTheme))
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

        override suspend fun setPromptSymbol(promptSymbol: PromptSymbol) {
            writeFailure?.let { throw it }
            this.promptSymbol = promptSymbol
            emit(mutablePreferences.value.copy(promptSymbol = promptSymbol))
        }

        override suspend fun setShowPromptPath(showPromptPath: Boolean) {
            writeFailure?.let { throw it }
            this.showPromptPath = showPromptPath
            emit(mutablePreferences.value.copy(showPromptPath = showPromptPath))
        }

        override suspend fun setDosDrive(dosDrive: DosDrive) {
            writeFailure?.let { throw it }
            this.dosDrive = dosDrive
            emit(mutablePreferences.value.copy(dosDrive = dosDrive))
        }

        override suspend fun pinPackage(packageName: String) = unsupported()

        override suspend fun unpinPackage(packageName: String) = unsupported()

        override suspend fun setAlias(name: String, packageName: String) = unsupported()

        override suspend fun recordLaunch(packageName: String, launchedAt: Long) = unsupported()

        private fun unsupported(): Nothing = error("Not required by this test")
    }

    private class RefreshRacePreferencesRepository : PreferencesRepository {
        private var preferencesValue = LauncherPreferences()
        private var subscriptions = 0
        val refreshStarted = CompletableDeferred<Unit>()
        val releaseRefresh = CompletableDeferred<Unit>()
        val releaseUsernameWrite = CompletableDeferred<Unit>()

        override val preferences: Flow<LauncherPreferences> = flow {
            subscriptions += 1
            if (subscriptions == 1) {
                emit(preferencesValue)
            } else {
                val snapshot = preferencesValue
                refreshStarted.complete(Unit)
                releaseRefresh.await()
                emit(snapshot)
            }
        }

        override suspend fun setShellType(shellType: ShellType) = unsupported()

        override suspend fun setTerminalTheme(terminalTheme: TerminalTheme) = unsupported()

        override suspend fun setShowClock(showClock: Boolean) {
            throw IOException("disk full")
        }

        override suspend fun setUsername(username: String) {
            releaseUsernameWrite.await()
            preferencesValue = preferencesValue.copy(username = username)
        }

        override suspend fun setHostname(hostname: String) = unsupported()

        override suspend fun setPromptSymbol(promptSymbol: PromptSymbol) = unsupported()

        override suspend fun setShowPromptPath(showPromptPath: Boolean) = unsupported()

        override suspend fun setDosDrive(dosDrive: DosDrive) = unsupported()

        override suspend fun pinPackage(packageName: String) = unsupported()

        override suspend fun unpinPackage(packageName: String) = unsupported()

        override suspend fun setAlias(name: String, packageName: String) = unsupported()

        override suspend fun recordLaunch(packageName: String, launchedAt: Long) = unsupported()

        private fun unsupported(): Nothing = error("Not required by this test")
    }
}
