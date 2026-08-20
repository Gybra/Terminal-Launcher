package com.gybra.terminallauncher.ui.home

import androidx.compose.ui.text.TextRange
import com.gybra.terminallauncher.MainDispatcherRule
import com.gybra.terminallauncher.command.Command
import com.gybra.terminallauncher.command.CommandExecutor
import com.gybra.terminallauncher.command.CommandResult
import com.gybra.terminallauncher.command.CommandRegistry
import com.gybra.terminallauncher.command.LauncherCommand
import com.gybra.terminallauncher.command.RecordingCommand
import com.gybra.terminallauncher.launcher.AppRepository
import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.launcher.FakePackageMonitor
import com.gybra.terminallauncher.launcher.LauncherClock
import com.gybra.terminallauncher.launcher.PackageChange
import com.gybra.terminallauncher.preferences.LauncherPreferences
import com.gybra.terminallauncher.preferences.RecordingPreferencesRepository
import com.gybra.terminallauncher.search.SearchResult
import com.gybra.terminallauncher.search.SearchResult.Match
import com.gybra.terminallauncher.shell.ShellType
import java.io.IOException
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
        val preferencesRepository = RecordingPreferencesRepository(
            initialPreferences = LauncherPreferences(
                pinnedPackages = setOf("com.example.mail", "com.example.removed"),
            ),
        )
        val viewModel = HomeViewModel(
            appRepository = FakeAppRepository(apps = apps),
            preferencesRepository = preferencesRepository,
            launcherClock = FakeLauncherClock(),
            commandExecutor = commandExecutor(),
            packageMonitor = FakePackageMonitor(),
        )
        startCollecting(viewModel)

        advanceUntilIdle()

        assertEquals(unixHomeState(apps = listOf(apps[1])), viewModel.uiState.value)
    }

    @Test
    fun `reacts when pinned package preferences change`() = runTest(mainDispatcherRule.dispatcher) {
        val app = InstalledApp(packageName = "com.example.mail", label = "Mail")
        val preferencesRepository = RecordingPreferencesRepository()
        val viewModel = HomeViewModel(
            appRepository = FakeAppRepository(apps = listOf(app)),
            preferencesRepository = preferencesRepository,
            launcherClock = FakeLauncherClock(),
            commandExecutor = commandExecutor(),
            packageMonitor = FakePackageMonitor(),
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
        val preferencesRepository = RecordingPreferencesRepository(
            initialPreferences = LauncherPreferences(
                pinnedPackages = setOf(app.packageName),
            ),
        )
        val viewModel = HomeViewModel(
            appRepository = FakeAppRepository(apps = listOf(app)),
            preferencesRepository = preferencesRepository,
            launcherClock = FakeLauncherClock(),
            commandExecutor = commandExecutor(),
            packageMonitor = FakePackageMonitor(),
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
        val preferencesRepository = RecordingPreferencesRepository()
        val viewModel = HomeViewModel(
            appRepository = FakeAppRepository(),
            preferencesRepository = preferencesRepository,
            launcherClock = FakeLauncherClock(),
            commandExecutor = commandExecutor(),
            packageMonitor = FakePackageMonitor(),
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
            preferencesRepository = RecordingPreferencesRepository(
                initialPreferences = LauncherPreferences(
                    pinnedPackages = setOf("com.example.mail"),
                ),
            ),
            launcherClock = FakeLauncherClock(),
            commandExecutor = commandExecutor(),
            packageMonitor = FakePackageMonitor(),
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
                    preferencesRepository = RecordingPreferencesRepository(),
                    launcherClock = FakeLauncherClock(),
                    commandExecutor = commandExecutor(),
                    packageMonitor = FakePackageMonitor(),
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
            preferencesRepository = RecordingPreferencesRepository(),
            launcherClock = clock,
            commandExecutor = commandExecutor(),
            packageMonitor = FakePackageMonitor(),
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
                preferencesRepository = RecordingPreferencesRepository(),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(),
                packageMonitor = FakePackageMonitor(),
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

            val actions = collectSubmittedActions(viewModel)
            viewModel.submitPrompt()
            advanceUntilIdle()

            assertEquals(listOf(SubmittedAction.LaunchApp(mailbox)), actions)
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

            val actions = collectSubmittedActions(viewModel)
            viewModel.submitPrompt()
            advanceUntilIdle()

            assertEquals(emptyList<SubmittedAction>(), actions)
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
                preferencesRepository = RecordingPreferencesRepository(),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(listApps),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(viewModel)
            advanceUntilIdle()

            viewModel.updatePromptValue(PromptState(input = "ls"))
            advanceUntilIdle()

            viewModel.submitPrompt()
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
                preferencesRepository = RecordingPreferencesRepository(),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(listApps),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(viewModel)
            advanceUntilIdle()

            viewModel.updatePromptValue(PromptState(input = "mail"))
            advanceUntilIdle()

            val actions = collectSubmittedActions(viewModel)
            viewModel.submitPrompt()
            advanceUntilIdle()

            assertEquals(listOf(SubmittedAction.LaunchApp(mail)), actions)
            assertEquals(0, listApps.executions)
        }

    @Test
    fun `records submitted input and command output in the terminal history`() =
        runTest(mainDispatcherRule.dispatcher) {
            val listApps = RecordingCommand(
                id = Command.LIST_APPS,
                result = CommandResult.Output(listOf("mail", "mailbox")),
            )
            val viewModel = HomeViewModel(
                appRepository = FakeAppRepository(apps = listOf(mail, mailbox)),
                preferencesRepository = RecordingPreferencesRepository(),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(listApps),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(viewModel)
            advanceUntilIdle()

            viewModel.updatePromptValue(PromptState(input = "ls"))
            advanceUntilIdle()
            viewModel.submitPrompt()
            advanceUntilIdle()

            assertEquals(
                listOf(TerminalEntry(id = 0L, input = "ls", output = listOf("mail", "mailbox"))),
                viewModel.uiState.value.history,
            )
        }

    @Test
    fun `records launched applications without output`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = searchingViewModel()
            startCollecting(viewModel)
            advanceUntilIdle()

            viewModel.updatePromptValue(PromptState(input = "mailbox"))
            advanceUntilIdle()
            viewModel.submitPrompt()
            advanceUntilIdle()

            assertEquals(
                listOf(TerminalEntry(id = 0L, input = "mailbox", output = emptyList())),
                viewModel.uiState.value.history,
            )
        }

    @Test
    fun `records nothing while the submitted query stays ambiguous`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = searchingViewModel()
            startCollecting(viewModel)
            advanceUntilIdle()

            viewModel.updatePromptValue(PromptState(input = "mailb"))
            advanceUntilIdle()
            viewModel.submitPrompt()
            advanceUntilIdle()

            assertEquals(emptyList<TerminalEntry>(), viewModel.uiState.value.history)
        }

    @Test
    fun `retains only the twenty most recent entries`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = searchingViewModel()
            startCollecting(viewModel)
            advanceUntilIdle()

            repeat(22) { submission ->
                viewModel.updatePromptValue(PromptState(input = "mailbox"))
                advanceUntilIdle()
                viewModel.submitPrompt()
                advanceUntilIdle()
                assertEquals(submission, viewModel.uiState.value.history.last().id.toInt())
            }

            val history = viewModel.uiState.value.history
            assertEquals(20, history.size)
            assertEquals(2L, history.first().id)
            assertEquals(21L, history.last().id)
        }

    @Test
    fun `erases the history without touching preferences when a command clears it`() =
        runTest(mainDispatcherRule.dispatcher) {
            val clear = RecordingCommand(id = Command.CLEAR, result = CommandResult.ClearHistory)
            val listApps = RecordingCommand(
                id = Command.LIST_APPS,
                result = CommandResult.Output(listOf("mail")),
            )
            val preferencesRepository = RecordingPreferencesRepository(
                initialPreferences = LauncherPreferences(pinnedPackages = setOf(mail.packageName)),
            )
            val viewModel = HomeViewModel(
                appRepository = FakeAppRepository(apps = listOf(mail)),
                preferencesRepository = preferencesRepository,
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(listApps, clear),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(viewModel)
            advanceUntilIdle()

            viewModel.updatePromptValue(PromptState(input = "ls"))
            advanceUntilIdle()
            viewModel.submitPrompt()
            advanceUntilIdle()
            assertEquals(1, viewModel.uiState.value.history.size)

            viewModel.updatePromptValue(PromptState(input = "clear"))
            advanceUntilIdle()
            viewModel.submitPrompt()
            advanceUntilIdle()

            assertEquals(emptyList<TerminalEntry>(), viewModel.uiState.value.history)
            assertEquals(PromptState(), viewModel.uiState.value.prompt)
            assertEquals(listOf(mail), viewModel.uiState.value.apps)
            assertEquals(emptyList<String>(), preferencesRepository.writes)
        }

    @Test
    fun `starts a new launcher session with an empty history`() =
        runTest(mainDispatcherRule.dispatcher) {
            val appRepository = FakeAppRepository(apps = listOf(mailbox))
            val preferencesRepository = RecordingPreferencesRepository()
            val first = HomeViewModel(
                appRepository = appRepository,
                preferencesRepository = preferencesRepository,
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(first)
            advanceUntilIdle()
            first.updatePromptValue(PromptState(input = "mailbox"))
            advanceUntilIdle()
            first.submitPrompt()
            advanceUntilIdle()
            assertEquals(1, first.uiState.value.history.size)

            val restarted = HomeViewModel(
                appRepository = appRepository,
                preferencesRepository = preferencesRepository,
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(restarted)
            advanceUntilIdle()

            assertEquals(emptyList<TerminalEntry>(), restarted.uiState.value.history)
        }

    @Test
    fun `gives commands every installed application, not only the pinned ones`() =
        runTest(mainDispatcherRule.dispatcher) {
            val listApps = RecordingCommand(id = Command.LIST_APPS)
            val viewModel = HomeViewModel(
                appRepository = FakeAppRepository(apps = listOf(mail, mailbox)),
                preferencesRepository = RecordingPreferencesRepository(
                    initialPreferences = LauncherPreferences(
                        pinnedPackages = setOf(mail.packageName),
                    ),
                ),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(listApps),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(viewModel)
            advanceUntilIdle()

            viewModel.updatePromptValue(PromptState(input = "ls"))
            advanceUntilIdle()
            viewModel.submitPrompt()
            advanceUntilIdle()

            assertEquals(listOf(mail), viewModel.uiState.value.apps)
            assertEquals(listOf(mail, mailbox), listApps.lastContext?.installedApps)
            assertEquals(UnixShellProfile, listApps.lastContext?.shellProfile)
        }

    @Test
    fun `unpins a package the system reports as removed`() =
        runTest(mainDispatcherRule.dispatcher) {
            val packageMonitor = FakePackageMonitor()
            val preferencesRepository = RecordingPreferencesRepository(
                initialPreferences = LauncherPreferences(pinnedPackages = setOf(mail.packageName)),
            )
            val viewModel = HomeViewModel(
                appRepository = FakeAppRepository(apps = listOf(mail)),
                preferencesRepository = preferencesRepository,
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(),
                packageMonitor = packageMonitor,
            )
            startCollecting(viewModel)
            advanceUntilIdle()

            packageMonitor.report(PackageChange(packageName = mail.packageName, removed = true))
            advanceUntilIdle()

            assertEquals(
                listOf("unpinPackage(${mail.packageName})"),
                preferencesRepository.writes,
            )
            assertEquals(emptyList<InstalledApp>(), viewModel.uiState.value.apps)
        }

    @Test
    fun `keeps the pin while a package is only replaced by an update`() =
        runTest(mainDispatcherRule.dispatcher) {
            val packageMonitor = FakePackageMonitor()
            val preferencesRepository = RecordingPreferencesRepository(
                initialPreferences = LauncherPreferences(pinnedPackages = setOf(mail.packageName)),
            )
            val viewModel = HomeViewModel(
                appRepository = FakeAppRepository(apps = listOf(mail)),
                preferencesRepository = preferencesRepository,
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(),
                packageMonitor = packageMonitor,
            )
            startCollecting(viewModel)
            advanceUntilIdle()

            packageMonitor.report(PackageChange(packageName = mail.packageName, removed = false))
            advanceUntilIdle()

            assertEquals(emptyList<String>(), preferencesRepository.writes)
            assertEquals(listOf(mail), viewModel.uiState.value.apps)
        }

    @Test
    fun `keeps Home working when unpinning a removed package fails`() =
        runTest(mainDispatcherRule.dispatcher) {
            val packageMonitor = FakePackageMonitor()
            val preferencesRepository = RecordingPreferencesRepository(
                initialPreferences = LauncherPreferences(pinnedPackages = setOf(mail.packageName)),
                writeFailure = IOException("storage unavailable"),
            )
            val viewModel = HomeViewModel(
                appRepository = FakeAppRepository(apps = listOf(mail)),
                preferencesRepository = preferencesRepository,
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(),
                packageMonitor = packageMonitor,
            )
            startCollecting(viewModel)
            advanceUntilIdle()

            packageMonitor.report(PackageChange(packageName = mail.packageName, removed = true))
            advanceUntilIdle()

            assertEquals(
                listOf("unpinPackage(${mail.packageName})"),
                preferencesRepository.writes,
            )

            packageMonitor.report(PackageChange(packageName = mailbox.packageName, removed = true))
            viewModel.updatePromptValue(PromptState(input = "mail"))
            advanceUntilIdle()

            assertEquals("mail", viewModel.uiState.value.prompt.input)
            assertEquals(listOf(mail), viewModel.uiState.value.searchResults.map(SearchResult::app))
        }

    @Test
    fun `asks the launcher to open settings when a command requests it`() =
        runTest(mainDispatcherRule.dispatcher) {
            val settings = RecordingCommand(
                id = Command.SETTINGS,
                result = CommandResult.OpenSettings,
            )
            val viewModel = HomeViewModel(
                appRepository = FakeAppRepository(apps = listOf(mail)),
                preferencesRepository = RecordingPreferencesRepository(),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(settings),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(viewModel)
            advanceUntilIdle()
            val actions = collectSubmittedActions(viewModel)

            viewModel.updatePromptValue(PromptState(input = "settings"))
            advanceUntilIdle()
            viewModel.submitPrompt()
            advanceUntilIdle()

            assertEquals(listOf(SubmittedAction.OpenSettings), actions)
            assertEquals(
                listOf(TerminalEntry(id = 0L, input = "settings", output = emptyList())),
                viewModel.uiState.value.history,
            )
            assertEquals(PromptState(), viewModel.uiState.value.prompt)
        }

    private fun TestScope.collectSubmittedActions(viewModel: HomeViewModel): List<SubmittedAction> {
        val actions = mutableListOf<SubmittedAction>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.submittedActions.collect { action -> actions += action }
        }
        return actions
    }

    private fun commandExecutor(vararg commands: LauncherCommand): CommandExecutor =
        CommandExecutor(CommandRegistry(commands = commands.toList()))

    private fun searchingViewModel(): HomeViewModel = HomeViewModel(
        appRepository = FakeAppRepository(apps = listOf(mailboxPro, mailbox, mail)),
        preferencesRepository = RecordingPreferencesRepository(),
        launcherClock = FakeLauncherClock(),
        commandExecutor = commandExecutor(),
        packageMonitor = FakePackageMonitor(),
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

}
