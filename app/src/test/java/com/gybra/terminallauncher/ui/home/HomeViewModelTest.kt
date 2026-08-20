package com.gybra.terminallauncher.ui.home

import androidx.compose.ui.text.TextRange
import com.gybra.terminallauncher.MainDispatcherRule
import com.gybra.terminallauncher.command.Command
import com.gybra.terminallauncher.command.CommandExecutor
import com.gybra.terminallauncher.command.CommandRegistry
import com.gybra.terminallauncher.command.LauncherCommand
import com.gybra.terminallauncher.command.RecordingCommand
import com.gybra.terminallauncher.launcher.AppRepository
import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.launcher.LauncherClock
import com.gybra.terminallauncher.preferences.LauncherPreferences
import com.gybra.terminallauncher.preferences.PreferencesRepository
import com.gybra.terminallauncher.search.SearchResult
import com.gybra.terminallauncher.search.SearchResult.Match
import com.gybra.terminallauncher.shell.ShellType
import com.gybra.terminallauncher.theme.TerminalTheme
import com.gybra.terminallauncher.shell.LauncherLocation
import com.gybra.terminallauncher.shell.ShellContext
import com.gybra.terminallauncher.shell.dos.DosShellProfile
import com.gybra.terminallauncher.shell.unix.UnixShellProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `shows only installed applications pinned in preferences`() = runTest(mainDispatcherRule.dispatcher) {
        val apps = listOf(
            InstalledApp(packageName = "com.example.browser", label = "Browser"),
            InstalledApp(packageName = "com.example.mail", label = "Mail"),
        )
        val preferencesRepository = FakePreferencesRepository(
            initialPreferences = LauncherPreferences(
                pinnedPackages = setOf("com.example.mail", "com.example.removed"),
            ),
        )
        val viewModel = HomeViewModel(
            appRepository = FakeAppRepository(apps = apps),
            preferencesRepository = preferencesRepository,
            launcherClock = FakeLauncherClock(),
            commandExecutor = commandExecutor(),
        )
        startCollecting(viewModel)

        advanceUntilIdle()

        assertEquals(unixHomeState(apps = listOf(apps[1])), viewModel.uiState.value)
    }

    @Test
    fun `reacts when pinned package preferences change`() = runTest(mainDispatcherRule.dispatcher) {
        val app = InstalledApp(packageName = "com.example.mail", label = "Mail")
        val preferencesRepository = FakePreferencesRepository()
        val viewModel = HomeViewModel(
            appRepository = FakeAppRepository(apps = listOf(app)),
            preferencesRepository = preferencesRepository,
            launcherClock = FakeLauncherClock(),
            commandExecutor = commandExecutor(),
        )
        startCollecting(viewModel)
        advanceUntilIdle()

        assertEquals(unixHomeState(), viewModel.uiState.value)

        preferencesRepository.emit(
            LauncherPreferences(pinnedPackages = setOf(app.packageName)),
        )
        advanceUntilIdle()

        assertEquals(unixHomeState(apps = listOf(app)), viewModel.uiState.value)
    }

    @Test
    fun `reacts when the selected shell changes`() = runTest(mainDispatcherRule.dispatcher) {
        val app = InstalledApp(packageName = "com.example.mail", label = "Mail")
        val preferencesRepository = FakePreferencesRepository(
            initialPreferences = LauncherPreferences(
                pinnedPackages = setOf(app.packageName),
            ),
        )
        val viewModel = HomeViewModel(
            appRepository = FakeAppRepository(apps = listOf(app)),
            preferencesRepository = preferencesRepository,
            launcherClock = FakeLauncherClock(),
            commandExecutor = commandExecutor(),
        )
        startCollecting(viewModel)
        advanceUntilIdle()

        preferencesRepository.emit(
            LauncherPreferences(
                shellType = ShellType.DOS,
                pinnedPackages = setOf(app.packageName),
            ),
        )
        advanceUntilIdle()

        assertEquals(
            HomeUiState(
                apps = listOf(app),
                shellProfile = DosShellProfile,
                shellContext = defaultShellContext(),
                clockText = "22:10",
            ),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `reacts when clock visibility and prompt identity change`() = runTest(mainDispatcherRule.dispatcher) {
        val preferencesRepository = FakePreferencesRepository()
        val viewModel = HomeViewModel(
            appRepository = FakeAppRepository(),
            preferencesRepository = preferencesRepository,
            launcherClock = FakeLauncherClock(),
            commandExecutor = commandExecutor(),
        )
        startCollecting(viewModel)
        advanceUntilIdle()

        preferencesRepository.emit(
            LauncherPreferences(
                showClock = false,
                username = "oreste",
                hostname = "phone",
            ),
        )
        advanceUntilIdle()

        assertEquals(
            HomeUiState(
                shellProfile = UnixShellProfile,
                shellContext = ShellContext(
                    username = "oreste",
                    hostname = "phone",
                    location = LauncherLocation.HOME,
                ),
                clockText = null,
            ),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `keeps an empty state when package access is denied`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = HomeViewModel(
            appRepository = FakeAppRepository(failure = SecurityException("denied")),
            preferencesRepository = FakePreferencesRepository(
                initialPreferences = LauncherPreferences(
                    pinnedPackages = setOf("com.example.mail"),
                ),
            ),
            launcherClock = FakeLauncherClock(),
            commandExecutor = commandExecutor(),
        )
        startCollecting(viewModel)

        advanceUntilIdle()

        assertEquals(unixHomeState(), viewModel.uiState.value)
    }

    @Test
    fun `propagates unexpected application repository failures`() {
        assertThrows(IllegalStateException::class.java) {
            runTest(mainDispatcherRule.dispatcher) {
                val viewModel = HomeViewModel(
                    appRepository = FakeAppRepository(
                        failure = IllegalStateException("broken repository"),
                    ),
                    preferencesRepository = FakePreferencesRepository(),
                    launcherClock = FakeLauncherClock(),
                    commandExecutor = commandExecutor(),
                )
                startCollecting(viewModel)

                advanceUntilIdle()
            }
        }
    }

    @Test
    fun `collects clock only while Home state has subscribers`() = runTest(mainDispatcherRule.dispatcher) {
        val clock = TrackingLauncherClock()
        val viewModel = HomeViewModel(
            appRepository = FakeAppRepository(),
            preferencesRepository = FakePreferencesRepository(),
            launcherClock = clock,
            commandExecutor = commandExecutor(),
        )

        runCurrent()
        assertEquals(0, clock.activeCollectors)

        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        runCurrent()
        assertEquals(1, clock.activeCollectors)

        collection.cancel()
        advanceTimeBy(5_000L)
        runCurrent()
        assertEquals(0, clock.activeCollectors)
    }

    @Test
    fun `updates prompt input and focus`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = HomeViewModel(
                appRepository = FakeAppRepository(),
                preferencesRepository = FakePreferencesRepository(),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(),
            )
            startCollecting(viewModel)
            advanceUntilIdle()

            viewModel.updatePromptValue(
                PromptState(
                    input = "telegram",
                    selection = TextRange(2, 5),
                    composition = TextRange(0, 8),
                ),
            )
            viewModel.updatePromptFocus(focused = true)
            advanceUntilIdle()

            assertEquals(
                PromptState(
                    input = "telegram",
                    selection = TextRange(2, 5),
                    composition = TextRange(0, 8),
                    focused = true,
                ),
                viewModel.uiState.value.prompt,
            )
        }

    @Test
    fun `ranks every installed application while the user types`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = searchingViewModel()
            startCollecting(viewModel)
            advanceUntilIdle()

            viewModel.updatePromptValue(PromptState(input = "mail"))
            advanceUntilIdle()

            assertEquals(
                listOf(
                    SearchResult(app = mail, match = Match.EXACT),
                    SearchResult(app = mailbox, match = Match.PREFIX),
                    SearchResult(app = mailboxPro, match = Match.PREFIX),
                ),
                viewModel.uiState.value.searchResults,
            )
        }

    @Test
    fun `clears the prompt and resolves the application on unambiguous submit`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = searchingViewModel()
            startCollecting(viewModel)
            advanceUntilIdle()

            viewModel.updatePromptValue(PromptState(input = "mailbox"))
            viewModel.updatePromptFocus(focused = true)
            advanceUntilIdle()

            assertEquals(mailbox, viewModel.submitPrompt())
            advanceUntilIdle()

            assertEquals(PromptState(focused = true), viewModel.uiState.value.prompt)
            assertEquals(emptyList<SearchResult>(), viewModel.uiState.value.searchResults)
        }

    @Test
    fun `keeps ambiguous results visible on submit`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = searchingViewModel()
            startCollecting(viewModel)
            advanceUntilIdle()

            viewModel.updatePromptValue(PromptState(input = "mailb"))
            advanceUntilIdle()

            assertNull(viewModel.submitPrompt())
            advanceUntilIdle()

            assertEquals(PromptState(input = "mailb"), viewModel.uiState.value.prompt)
            assertEquals(
                listOf(mailbox, mailboxPro),
                viewModel.uiState.value.searchResults.map(SearchResult::app),
            )
        }

    private val mail = InstalledApp(packageName = "com.example.mail", label = "Mail")
    private val mailbox = InstalledApp(packageName = "com.example.mailbox", label = "Mailbox")
    private val mailboxPro = InstalledApp(packageName = "com.example.pro", label = "Mailbox Pro")

    private class FakeAppRepository(
        private val apps: List<InstalledApp> = emptyList(),
        private val failure: Throwable? = null,
    ) : AppRepository {
        override suspend fun getInstalledApps(): List<InstalledApp> {
            failure?.let { throw it }
            return apps
        }

        override fun observeInstalledApps(): Flow<List<InstalledApp>> = flow {
            failure?.let { throw it }
            emit(apps)
        }
    }

    @Test
    fun `runs a registered command instead of searching and clears the prompt`() =
        runTest(mainDispatcherRule.dispatcher) {
            val listApps = RecordingCommand(id = Command.LIST_APPS)
            val viewModel = HomeViewModel(
                appRepository = FakeAppRepository(apps = listOf(mail)),
                preferencesRepository = FakePreferencesRepository(),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(listApps),
            )
            startCollecting(viewModel)
            advanceUntilIdle()

            viewModel.updatePromptValue(PromptState(input = "ls"))
            advanceUntilIdle()

            assertNull(viewModel.submitPrompt())
            advanceUntilIdle()

            assertEquals(1, listApps.executions)
            assertEquals(PromptState(), viewModel.uiState.value.prompt)
        }

    @Test
    fun `searches instead of running a command the active shell does not know`() =
        runTest(mainDispatcherRule.dispatcher) {
            val listApps = RecordingCommand(id = Command.LIST_APPS)
            val viewModel = HomeViewModel(
                appRepository = FakeAppRepository(apps = listOf(mail)),
                preferencesRepository = FakePreferencesRepository(),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(listApps),
            )
            startCollecting(viewModel)
            advanceUntilIdle()

            viewModel.updatePromptValue(PromptState(input = "mail"))
            advanceUntilIdle()

            assertEquals(mail, viewModel.submitPrompt())
            assertEquals(0, listApps.executions)
        }

    private fun commandExecutor(vararg commands: LauncherCommand): CommandExecutor =
        CommandExecutor(CommandRegistry(commands = commands.toList()))

    private fun searchingViewModel(): HomeViewModel = HomeViewModel(
        appRepository = FakeAppRepository(apps = listOf(mailboxPro, mailbox, mail)),
        preferencesRepository = FakePreferencesRepository(),
        launcherClock = FakeLauncherClock(),
        commandExecutor = commandExecutor(),
    )

    private fun unixHomeState(apps: List<InstalledApp> = emptyList()): HomeUiState = HomeUiState(
        shellProfile = UnixShellProfile,
        shellContext = defaultShellContext(),
        apps = apps,
        clockText = "22:10",
    )

    private fun defaultShellContext(): ShellContext = ShellContext(
        username = "user",
        hostname = "android",
        location = LauncherLocation.HOME,
    )

    private fun TestScope.startCollecting(viewModel: HomeViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
    }

    private class FakeLauncherClock : LauncherClock {
        override fun observeTime(): Flow<String> = flowOf("22:10")
    }

    private class TrackingLauncherClock : LauncherClock {
        var activeCollectors = 0

        override fun observeTime(): Flow<String> = flow {
            activeCollectors += 1
            try {
                emit("22:10")
                awaitCancellation()
            } finally {
                activeCollectors -= 1
            }
        }
    }

    private class FakePreferencesRepository(
        initialPreferences: LauncherPreferences = LauncherPreferences(),
    ) : PreferencesRepository {
        private val mutablePreferences = MutableStateFlow(initialPreferences)
        override val preferences: Flow<LauncherPreferences> = mutablePreferences

        fun emit(preferences: LauncherPreferences) {
            mutablePreferences.value = preferences
        }

        override suspend fun setShellType(shellType: ShellType) = unsupported()

        override suspend fun setTerminalTheme(terminalTheme: TerminalTheme) = unsupported()

        override suspend fun setShowClock(showClock: Boolean) = unsupported()

        override suspend fun setUsername(username: String) = unsupported()

        override suspend fun setHostname(hostname: String) = unsupported()

        override suspend fun pinPackage(packageName: String) = unsupported()

        override suspend fun unpinPackage(packageName: String) = unsupported()

        private fun unsupported(): Nothing = error("Not required by this test")
    }
}
