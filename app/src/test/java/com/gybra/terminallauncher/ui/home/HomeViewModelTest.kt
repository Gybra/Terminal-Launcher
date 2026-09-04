package com.gybra.terminallauncher.ui.home

import androidx.compose.ui.text.TextRange
import com.gybra.terminallauncher.MainDispatcherRule
import com.gybra.terminallauncher.command.Command
import com.gybra.terminallauncher.command.CommandContext
import com.gybra.terminallauncher.command.CommandExecutor
import com.gybra.terminallauncher.command.CommandGroup
import com.gybra.terminallauncher.command.CommandResult
import com.gybra.terminallauncher.command.CommandRegistry
import com.gybra.terminallauncher.command.LauncherCommand
import com.gybra.terminallauncher.command.RecordingCommand
import com.gybra.terminallauncher.launcher.BatteryStatus
import com.gybra.terminallauncher.launcher.FakeAppRepository
import com.gybra.terminallauncher.launcher.FakeBatteryRepository
import com.gybra.terminallauncher.launcher.FakeLauncherClock
import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.launcher.AppShortcut
import com.gybra.terminallauncher.launcher.SystemScreen
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
import com.gybra.terminallauncher.shell.DosDrive
import com.gybra.terminallauncher.shell.LauncherLocation
import com.gybra.terminallauncher.shell.PromptSymbol
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
        val preferencesRepository = RecordingPreferencesRepository(
            initialPreferences = LauncherPreferences(
                pinnedPackages = setOf("com.example.mail", "com.example.removed"),
            ),
        )
        val viewModel = HomeViewModel(
            appRepository = FakeAppRepository(apps = apps),
            preferencesRepository = preferencesRepository,
            batteryRepository = FakeBatteryRepository(status = null),
            launcherClock = FakeLauncherClock(),
            commandExecutor = commandExecutor(),
            packageMonitor = FakePackageMonitor(),
        )
        startCollecting(viewModel)

        advanceUntilIdle()

        assertEquals(
            unixHomeState(apps = listOf(apps[1]), helpInvitation = null),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `reacts when pinned package preferences change`() = runTest(mainDispatcherRule.dispatcher) {
        val app = InstalledApp(packageName = "com.example.mail", label = "Mail")
        val preferencesRepository = RecordingPreferencesRepository()
        val viewModel = HomeViewModel(
            appRepository = FakeAppRepository(apps = listOf(app)),
            preferencesRepository = preferencesRepository,
            batteryRepository = FakeBatteryRepository(status = null),
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

        assertEquals(unixHomeState(apps = listOf(app), helpInvitation = null), viewModel.uiState.value)
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
            batteryRepository = FakeBatteryRepository(status = null),
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
                statusClock = "22:10",
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
            batteryRepository = FakeBatteryRepository(status = null),
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
                helpInvitation = HELP_INVITATION,
                statusClock = null,
            ),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `writes the clock and the battery as the two sides of the status line`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = HomeViewModel(
                appRepository = FakeAppRepository(),
                preferencesRepository = RecordingPreferencesRepository(),
                batteryRepository = FakeBatteryRepository(
                    status = BatteryStatus(percentage = 42, charging = true),
                ),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(viewModel)
            advanceUntilIdle()

            assertEquals("22:10", viewModel.uiState.value.statusClock)
            assertEquals("42% charging", viewModel.uiState.value.statusBattery)
        }

    @Test
    fun `follows the battery and leaves out what the preferences hide`() =
        runTest(mainDispatcherRule.dispatcher) {
            val batteryRepository = FakeBatteryRepository(
                status = BatteryStatus(percentage = 42, charging = false),
            )
            val preferencesRepository = RecordingPreferencesRepository()
            val viewModel = HomeViewModel(
                appRepository = FakeAppRepository(),
                preferencesRepository = preferencesRepository,
                batteryRepository = batteryRepository,
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(viewModel)
            advanceUntilIdle()

            assertEquals("42%", viewModel.uiState.value.statusBattery)

            batteryRepository.emit(BatteryStatus(percentage = 41, charging = false))
            advanceUntilIdle()

            assertEquals("41%", viewModel.uiState.value.statusBattery)

            preferencesRepository.emit(LauncherPreferences(showBattery = false))
            advanceUntilIdle()

            assertEquals("22:10", viewModel.uiState.value.statusClock)
            assertNull(viewModel.uiState.value.statusBattery)

            preferencesRepository.emit(LauncherPreferences(showClock = false))
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.statusClock)
            assertEquals("41%", viewModel.uiState.value.statusBattery)

            batteryRepository.emit(null)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.statusBattery)
        }

    @Test
    fun `carries the cosmetic prompt preferences into the shell context`() =
        runTest(mainDispatcherRule.dispatcher) {
            val preferencesRepository = RecordingPreferencesRepository()
            val viewModel = HomeViewModel(
                appRepository = FakeAppRepository(),
                preferencesRepository = preferencesRepository,
                batteryRepository = FakeBatteryRepository(status = null),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(viewModel)
            advanceUntilIdle()

            preferencesRepository.emit(
                LauncherPreferences(
                    promptSymbol = PromptSymbol.PERCENT,
                    showPromptPath = false,
                    dosDrive = DosDrive.D,
                ),
            )
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(
                ShellContext(
                    username = "user",
                    hostname = "android",
                    location = LauncherLocation.HOME,
                    promptSymbol = PromptSymbol.PERCENT,
                    showPath = false,
                    dosDrive = DosDrive.D,
                ),
                state.shellContext,
            )
            assertEquals("user@android%", state.shellProfile.prompt(state.shellContext))
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
            batteryRepository = FakeBatteryRepository(status = null),
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
                    batteryRepository = FakeBatteryRepository(status = null),
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
            batteryRepository = FakeBatteryRepository(status = null),
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
                batteryRepository = FakeBatteryRepository(status = null),
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

            viewModel.type("mail")
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

            viewModel.type("mailbox")
            viewModel.updatePromptFocus(focused = true)
            advanceUntilIdle()

            val actions = collectSubmittedActions(viewModel)
            viewModel.submitPrompt()
            advanceUntilIdle()

            assertEquals(listOf(SubmittedAction.LaunchApp(mailbox)), actions)
            assertEquals("", viewModel.uiState.value.prompt.input)
            assertEquals(true, viewModel.uiState.value.prompt.focused)
            assertEquals(emptyList<SearchResult>(), viewModel.uiState.value.searchResults)
        }

    @Test
    fun `consumes an ambiguous line and keeps its candidates startable`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = searchingViewModel()
            startCollecting(viewModel)
            advanceUntilIdle()

            viewModel.type("mailb")
            advanceUntilIdle()

            val actions = collectSubmittedActions(viewModel)
            viewModel.submitPrompt()
            advanceUntilIdle()

            assertEquals(emptyList<SubmittedAction>(), actions)
            assertEquals("", viewModel.uiState.value.prompt.input)
            assertEquals(
                listOf(
                    TerminalEntry(
                        id = 0L,
                        input = "mailb",
                        output = listOf("mailb matches more than one application"),
                        apps = listOf(mailbox, mailboxPro),
                    ),
                ),
                viewModel.uiState.value.history,
            )
        }

    @Test
    fun `offers pin and uninstall when an application is held, leaving the prompt alone`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = HomeViewModel(
                appRepository = FakeAppRepository(apps = listOf(mail)),
                preferencesRepository = RecordingPreferencesRepository(),
                batteryRepository = FakeBatteryRepository(status = null),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(viewModel)
            advanceUntilIdle()

            viewModel.offerAppCommands(mail, rowKey = mail.packageName)
            advanceUntilIdle()

            assertEquals(
                listOf(
                    HoldChoice(label = "pin", line = "pin Mail"),
                    HoldChoice(label = "uninstall", line = "uninstall Mail"),
                ),
                viewModel.uiState.value.holdChoices,
            )
            assertEquals(mail.packageName, viewModel.uiState.value.holdRowKey)
            assertEquals("", viewModel.uiState.value.prompt.input)
        }

    @Test
    fun `offers unpin and uninstall when a pinned application is held`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = HomeViewModel(
                appRepository = FakeAppRepository(apps = listOf(mail)),
                preferencesRepository = RecordingPreferencesRepository(
                    initialPreferences = LauncherPreferences(pinnedPackages = setOf(mail.packageName)),
                ),
                batteryRepository = FakeBatteryRepository(status = null),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(viewModel)
            advanceUntilIdle()

            viewModel.offerAppCommands(mail, rowKey = mail.packageName)
            advanceUntilIdle()

            assertEquals(
                listOf(
                    HoldChoice(label = "unpin", line = "unpin Mail"),
                    HoldChoice(label = "uninstall", line = "uninstall Mail"),
                ),
                viewModel.uiState.value.holdChoices,
            )
        }

    @Test
    fun `offers the hold commands in the shell case`() =
        runTest(mainDispatcherRule.dispatcher) {
            val preferencesRepository = RecordingPreferencesRepository()
            val viewModel = HomeViewModel(
                appRepository = FakeAppRepository(apps = listOf(mail)),
                preferencesRepository = preferencesRepository,
                batteryRepository = FakeBatteryRepository(status = null),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(viewModel)
            advanceUntilIdle()

            preferencesRepository.emit(LauncherPreferences(shellType = ShellType.DOS))
            advanceUntilIdle()
            viewModel.offerAppCommands(mail, rowKey = mail.packageName)
            advanceUntilIdle()

            assertEquals(
                listOf(
                    HoldChoice(label = "PIN", line = "PIN Mail"),
                    HoldChoice(label = "UNINSTALL", line = "UNINSTALL Mail"),
                ),
                viewModel.uiState.value.holdChoices,
            )
        }

    @Test
    fun `writes the chosen command at the prompt and dismisses the choices`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = HomeViewModel(
                appRepository = FakeAppRepository(apps = listOf(mail)),
                preferencesRepository = RecordingPreferencesRepository(),
                batteryRepository = FakeBatteryRepository(status = null),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(viewModel)
            advanceUntilIdle()
            viewModel.offerAppCommands(mail, rowKey = mail.packageName)
            advanceUntilIdle()

            val uninstall = viewModel.uiState.value.holdChoices.last()
            viewModel.writeChoice(uninstall)
            advanceUntilIdle()

            assertEquals(
                PromptState(
                    input = "uninstall Mail",
                    selection = TextRange("uninstall Mail".length),
                ),
                viewModel.uiState.value.prompt,
            )
            assertEquals(emptyList<HoldChoice>(), viewModel.uiState.value.holdChoices)
        }

    @Test
    fun `leaves the prompt alone when the hold is dismissed`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = HomeViewModel(
                appRepository = FakeAppRepository(apps = listOf(mail)),
                preferencesRepository = RecordingPreferencesRepository(),
                batteryRepository = FakeBatteryRepository(status = null),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(viewModel)
            advanceUntilIdle()
            viewModel.type("mail")
            viewModel.offerAppCommands(mail, rowKey = mail.packageName)
            advanceUntilIdle()

            viewModel.dismissChoices()
            advanceUntilIdle()

            assertEquals("mail", viewModel.uiState.value.prompt.input)
            assertEquals(emptyList<HoldChoice>(), viewModel.uiState.value.holdChoices)
            assertEquals(null, viewModel.uiState.value.holdRowKey)
        }

    @Test
    fun `writes the shortcuts command a held shortcut offers`() =
        runTest(mainDispatcherRule.dispatcher) {
            val preferencesRepository = RecordingPreferencesRepository(
                initialPreferences = LauncherPreferences(pinnedShortcuts = listOf(inbox)),
            )
            val viewModel = HomeViewModel(
                appRepository = FakeAppRepository(apps = listOf(mail)),
                preferencesRepository = preferencesRepository,
                batteryRepository = FakeBatteryRepository(status = null),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(viewModel)
            advanceUntilIdle()

            viewModel.writeShortcutCommand(inbox)
            advanceUntilIdle()
            assertEquals("shortcuts unpin Mail Inbox", viewModel.uiState.value.prompt.input)

            preferencesRepository.emit(LauncherPreferences())
            advanceUntilIdle()
            viewModel.writeShortcutCommand(inbox)
            advanceUntilIdle()

            assertEquals("shortcuts pin Mail Inbox", viewModel.uiState.value.prompt.input)
        }

    @Test
    fun `leaves the prompt alone when a held shortcut has lost its application`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = HomeViewModel(
                appRepository = FakeAppRepository(apps = emptyList()),
                preferencesRepository = RecordingPreferencesRepository(
                    initialPreferences = LauncherPreferences(pinnedShortcuts = listOf(inbox)),
                ),
                batteryRepository = FakeBatteryRepository(status = null),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(viewModel)
            advanceUntilIdle()

            viewModel.writeShortcutCommand(inbox)
            advanceUntilIdle()

            assertEquals("", viewModel.uiState.value.prompt.input)
        }

    @Test
    fun `invites the help command while Home holds nothing`() =
        runTest(mainDispatcherRule.dispatcher) {
            val preferencesRepository = RecordingPreferencesRepository()
            val viewModel = HomeViewModel(
                appRepository = FakeAppRepository(apps = listOf(mail)),
                preferencesRepository = preferencesRepository,
                batteryRepository = FakeBatteryRepository(status = null),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(viewModel)
            advanceUntilIdle()

            assertEquals(HELP_INVITATION, viewModel.uiState.value.helpInvitation)

            preferencesRepository.emit(LauncherPreferences(shellType = ShellType.DOS))
            advanceUntilIdle()

            assertEquals("TYPE HELP TO LIST THE COMMANDS", viewModel.uiState.value.helpInvitation)
        }

    @Test
    fun `takes the invitation back as soon as Home holds something`() =
        runTest(mainDispatcherRule.dispatcher) {
            val preferencesRepository = RecordingPreferencesRepository()
            val viewModel = HomeViewModel(
                appRepository = FakeAppRepository(apps = listOf(mail)),
                preferencesRepository = preferencesRepository,
                batteryRepository = FakeBatteryRepository(status = null),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(viewModel)
            advanceUntilIdle()

            preferencesRepository.emit(
                LauncherPreferences(pinnedPackages = setOf(mail.packageName)),
            )
            advanceUntilIdle()
            assertNull(viewModel.uiState.value.helpInvitation)

            preferencesRepository.emit(LauncherPreferences(pinnedShortcuts = listOf(inbox)))
            advanceUntilIdle()
            assertNull(viewModel.uiState.value.helpInvitation)

            preferencesRepository.emit(LauncherPreferences())
            advanceUntilIdle()
            viewModel.type("telegram")
            advanceUntilIdle()
            viewModel.submitPrompt()
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.helpInvitation)
        }

    @Test
    fun `writes the invitation again once a command clears an empty Home`() =
        runTest(mainDispatcherRule.dispatcher) {
            val clear = RecordingCommand(id = Command.CLEAR, result = CommandResult.ClearHistory)
            val viewModel = HomeViewModel(
                appRepository = FakeAppRepository(apps = listOf(mail)),
                preferencesRepository = RecordingPreferencesRepository(),
                batteryRepository = FakeBatteryRepository(status = null),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(clear),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(viewModel)
            advanceUntilIdle()

            viewModel.type("telegram")
            advanceUntilIdle()
            viewModel.submitPrompt()
            advanceUntilIdle()
            assertNull(viewModel.uiState.value.helpInvitation)

            viewModel.type("clear")
            advanceUntilIdle()
            viewModel.submitPrompt()
            advanceUntilIdle()

            assertEquals(HELP_INVITATION, viewModel.uiState.value.helpInvitation)
        }

    @Test
    fun `clears the prompt when a row starts something and keeps the history`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = searchingViewModel()
            startCollecting(viewModel)
            advanceUntilIdle()

            viewModel.type("telegram")
            advanceUntilIdle()
            viewModel.submitPrompt()
            advanceUntilIdle()
            viewModel.type("mailb")
            viewModel.updatePromptFocus(focused = true)
            advanceUntilIdle()

            viewModel.clearPrompt()
            advanceUntilIdle()

            assertEquals("", viewModel.uiState.value.prompt.input)
            assertEquals(true, viewModel.uiState.value.prompt.focused)
            assertEquals(1, viewModel.uiState.value.history.size)
        }

    private val mail = InstalledApp(packageName = "com.example.mail", label = "Mail")
    private val mailbox = InstalledApp(packageName = "com.example.mailbox", label = "Mailbox")
    private val mailboxPro = InstalledApp(packageName = "com.example.pro", label = "Mailbox Pro")
    private val inbox = AppShortcut(
        packageName = "com.example.mail",
        id = "inbox",
        label = "Inbox",
    )

    @Test
    fun `runs a registered command instead of searching and clears the prompt`() =
        runTest(mainDispatcherRule.dispatcher) {
            val listApps = RecordingCommand(id = Command.LIST_APPS)
            val viewModel = HomeViewModel(
                appRepository = FakeAppRepository(apps = listOf(mail)),
                preferencesRepository = RecordingPreferencesRepository(),
                batteryRepository = FakeBatteryRepository(status = null),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(listApps),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(viewModel)
            advanceUntilIdle()

            viewModel.type("ls")
            advanceUntilIdle()

            viewModel.submitPrompt()
            advanceUntilIdle()

            assertEquals(1, listApps.executions)
            assertEquals("", viewModel.uiState.value.prompt.input)
        }

    @Test
    fun `clears the prompt as soon as the line is submitted`() =
        runTest(mainDispatcherRule.dispatcher) {
            val hang = object : LauncherCommand {
                override val id: Command = Command.LIST_APPS
                override val group: CommandGroup = CommandGroup.APPS
                override val description: String = "Hang"
                override suspend fun execute(context: CommandContext): CommandResult {
                    awaitCancellation()
                }
            }
            val viewModel = HomeViewModel(
                appRepository = FakeAppRepository(apps = listOf(mail)),
                preferencesRepository = RecordingPreferencesRepository(),
                batteryRepository = FakeBatteryRepository(status = null),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(hang),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(viewModel)
            advanceUntilIdle()

            viewModel.type("ls")
            advanceUntilIdle()
            viewModel.submitPrompt()
            runCurrent()

            assertEquals("", viewModel.uiState.value.prompt.input)
        }

    @Test
    fun `drops a stale IME edit after the prompt was cleared`() =
        runTest(mainDispatcherRule.dispatcher) {
            val listApps = RecordingCommand(id = Command.LIST_APPS)
            val viewModel = HomeViewModel(
                appRepository = FakeAppRepository(apps = listOf(mail)),
                preferencesRepository = RecordingPreferencesRepository(),
                batteryRepository = FakeBatteryRepository(status = null),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(listApps),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(viewModel)
            advanceUntilIdle()

            viewModel.type("ls")
            advanceUntilIdle()
            viewModel.submitPrompt()
            runCurrent()
            viewModel.updatePromptValue(PromptState(input = "ls", generation = 0))

            assertEquals("", viewModel.uiState.value.prompt.input)
        }

    @Test
    fun `searches instead of running a command the active shell does not know`() =
        runTest(mainDispatcherRule.dispatcher) {
            val listApps = RecordingCommand(id = Command.LIST_APPS)
            val viewModel = HomeViewModel(
                appRepository = FakeAppRepository(apps = listOf(mail)),
                preferencesRepository = RecordingPreferencesRepository(),
                batteryRepository = FakeBatteryRepository(status = null),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(listApps),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(viewModel)
            advanceUntilIdle()

            viewModel.type("mail")
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
                batteryRepository = FakeBatteryRepository(status = null),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(listApps),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(viewModel)
            advanceUntilIdle()

            viewModel.type("ls")
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

            viewModel.type("mailbox")
            advanceUntilIdle()
            viewModel.submitPrompt()
            advanceUntilIdle()

            assertEquals(
                listOf(TerminalEntry(id = 0L, input = "mailbox", output = emptyList())),
                viewModel.uiState.value.history,
            )
        }

    @Test
    fun `consumes a line matching no application and answers it`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = searchingViewModel()
            startCollecting(viewModel)
            advanceUntilIdle()

            viewModel.type("telegram")
            advanceUntilIdle()
            viewModel.submitPrompt()
            advanceUntilIdle()

            assertEquals("", viewModel.uiState.value.prompt.input)
            assertEquals(
                listOf(
                    TerminalEntry(
                        id = 0L,
                        input = "telegram",
                        output = listOf("no application matches telegram"),
                    ),
                ),
                viewModel.uiState.value.history,
            )
        }

    @Test
    fun `leaves an empty line alone instead of answering it`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = searchingViewModel()
            startCollecting(viewModel)
            advanceUntilIdle()

            viewModel.type("   ")
            advanceUntilIdle()
            viewModel.submitPrompt()
            advanceUntilIdle()

            assertEquals(emptyList<TerminalEntry>(), viewModel.uiState.value.history)
            assertEquals(PromptState(input = "   "), viewModel.uiState.value.prompt)
        }

    @Test
    fun `retains only the twenty most recent entries`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = searchingViewModel()
            startCollecting(viewModel)
            advanceUntilIdle()

            repeat(22) { submission ->
                viewModel.type("mailbox")
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
                batteryRepository = FakeBatteryRepository(status = null),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(listApps, clear),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(viewModel)
            advanceUntilIdle()

            viewModel.type("ls")
            advanceUntilIdle()
            viewModel.submitPrompt()
            advanceUntilIdle()
            assertEquals(1, viewModel.uiState.value.history.size)

            viewModel.type("clear")
            advanceUntilIdle()
            viewModel.submitPrompt()
            advanceUntilIdle()

            assertEquals(emptyList<TerminalEntry>(), viewModel.uiState.value.history)
            assertEquals("", viewModel.uiState.value.prompt.input)
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
                batteryRepository = FakeBatteryRepository(status = null),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(first)
            advanceUntilIdle()
            first.type("mailbox")
            advanceUntilIdle()
            first.submitPrompt()
            advanceUntilIdle()
            assertEquals(1, first.uiState.value.history.size)

            val restarted = HomeViewModel(
                appRepository = appRepository,
                preferencesRepository = preferencesRepository,
                batteryRepository = FakeBatteryRepository(status = null),
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
                batteryRepository = FakeBatteryRepository(status = null),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(listApps),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(viewModel)
            advanceUntilIdle()

            viewModel.type("ls")
            advanceUntilIdle()
            viewModel.submitPrompt()
            advanceUntilIdle()

            assertEquals(listOf(mail), viewModel.uiState.value.apps)
            assertEquals(listOf(mail, mailbox), listApps.lastContext?.installedApps)
            assertEquals(UnixShellProfile, listApps.lastContext?.shellProfile)
        }

    @Test
    fun `launches the application an alias names`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = HomeViewModel(
                appRepository = FakeAppRepository(apps = listOf(mail, mailbox)),
                preferencesRepository = RecordingPreferencesRepository(
                    initialPreferences = LauncherPreferences(
                        aliases = mapOf("m" to mailbox.packageName),
                    ),
                ),
                batteryRepository = FakeBatteryRepository(status = null),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(viewModel)
            advanceUntilIdle()
            val actions = collectSubmittedActions(viewModel)

            viewModel.type(" M ")
            advanceUntilIdle()
            viewModel.submitPrompt()
            advanceUntilIdle()

            assertEquals(listOf(SubmittedAction.LaunchApp(mailbox)), actions)
        }

    @Test
    fun `searches instead when an alias names an application that is gone`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = HomeViewModel(
                appRepository = FakeAppRepository(apps = listOf(mail)),
                preferencesRepository = RecordingPreferencesRepository(
                    initialPreferences = LauncherPreferences(
                        aliases = mapOf("mail" to "com.example.removed"),
                    ),
                ),
                batteryRepository = FakeBatteryRepository(status = null),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(viewModel)
            advanceUntilIdle()
            val actions = collectSubmittedActions(viewModel)

            viewModel.type("mail")
            advanceUntilIdle()
            viewModel.submitPrompt()
            advanceUntilIdle()

            assertEquals(listOf(SubmittedAction.LaunchApp(mail)), actions)
        }

    @Test
    fun `records the launch of the application it resolves`() =
        runTest(mainDispatcherRule.dispatcher) {
            val preferencesRepository = RecordingPreferencesRepository()
            val viewModel = HomeViewModel(
                appRepository = FakeAppRepository(apps = listOf(mail, mailbox)),
                preferencesRepository = preferencesRepository,
                batteryRepository = FakeBatteryRepository(status = null),
                launcherClock = FakeLauncherClock(epochMillis = 1_234L),
                commandExecutor = commandExecutor(),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(viewModel)
            advanceUntilIdle()
            val actions = collectSubmittedActions(viewModel)

            viewModel.type("mailbox")
            advanceUntilIdle()
            viewModel.submitPrompt()
            advanceUntilIdle()

            assertEquals(
                listOf("recordLaunch(${mailbox.packageName}, 1234)"),
                preferencesRepository.writes,
            )
            assertEquals(listOf(SubmittedAction.LaunchApp(mailbox)), actions)
        }

    @Test
    fun `launches the application even when recording the launch fails`() =
        runTest(mainDispatcherRule.dispatcher) {
            val preferencesRepository = RecordingPreferencesRepository(
                writeFailure = IOException("storage unavailable"),
            )
            val viewModel = HomeViewModel(
                appRepository = FakeAppRepository(apps = listOf(mail, mailbox)),
                preferencesRepository = preferencesRepository,
                batteryRepository = FakeBatteryRepository(status = null),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(viewModel)
            advanceUntilIdle()
            val actions = collectSubmittedActions(viewModel)

            viewModel.type("mailbox")
            advanceUntilIdle()
            viewModel.submitPrompt()
            advanceUntilIdle()

            assertEquals(listOf(SubmittedAction.LaunchApp(mailbox)), actions)
        }

    @Test
    fun `hands a system destination and a restart to the composition root`() =
        runTest(mainDispatcherRule.dispatcher) {
            val wifi = RecordingCommand(
                id = Command.WIFI_SETTINGS,
                result = CommandResult.OpenSystemScreen(SystemScreen.WifiSettings),
            )
            val restart = RecordingCommand(
                id = Command.RESTART,
                result = CommandResult.RestartLauncher,
            )
            val viewModel = HomeViewModel(
                appRepository = FakeAppRepository(apps = listOf(mail)),
                preferencesRepository = RecordingPreferencesRepository(),
                batteryRepository = FakeBatteryRepository(status = null),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(wifi, restart),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(viewModel)
            advanceUntilIdle()
            val actions = collectSubmittedActions(viewModel)

            viewModel.type("wifi")
            advanceUntilIdle()
            viewModel.submitPrompt()
            advanceUntilIdle()
            viewModel.type("restart")
            advanceUntilIdle()
            viewModel.submitPrompt()
            advanceUntilIdle()

            assertEquals(
                listOf(
                    SubmittedAction.OpenSystemScreen(SystemScreen.WifiSettings),
                    SubmittedAction.RestartLauncher,
                ),
                actions,
            )
            assertEquals(
                listOf("wifi", "restart"),
                viewModel.uiState.value.history.map(TerminalEntry::input),
            )
        }

    @Test
    fun `unpins a package the system reports as removed`() =
        runTest(mainDispatcherRule.dispatcher) {
            val packageMonitor = FakePackageMonitor()
            val preferencesRepository = RecordingPreferencesRepository(
                initialPreferences = LauncherPreferences(
                    pinnedPackages = setOf(mail.packageName),
                    pinnedShortcuts = listOf(inbox),
                ),
            )
            val viewModel = HomeViewModel(
                appRepository = FakeAppRepository(apps = listOf(mail)),
                preferencesRepository = preferencesRepository,
                batteryRepository = FakeBatteryRepository(status = null),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(),
                packageMonitor = packageMonitor,
            )
            startCollecting(viewModel)
            advanceUntilIdle()

            packageMonitor.report(PackageChange(packageName = mail.packageName, removed = true))
            advanceUntilIdle()

            assertEquals(
                listOf("unpinPackage(${mail.packageName})", "unpinShortcuts(${mail.packageName})"),
                preferencesRepository.writes,
            )
            assertEquals(emptyList<InstalledApp>(), viewModel.uiState.value.apps)
            assertEquals(emptyList<AppShortcut>(), viewModel.uiState.value.shortcuts)
        }

    @Test
    fun `lists the shortcuts pinned to Home`() =
        runTest(mainDispatcherRule.dispatcher) {
            val preferencesRepository = RecordingPreferencesRepository(
                initialPreferences = LauncherPreferences(pinnedShortcuts = listOf(inbox)),
            )
            val viewModel = HomeViewModel(
                appRepository = FakeAppRepository(apps = listOf(mail)),
                preferencesRepository = preferencesRepository,
                batteryRepository = FakeBatteryRepository(status = null),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(viewModel)
            advanceUntilIdle()

            assertEquals(listOf(inbox), viewModel.uiState.value.shortcuts)
        }

    @Test
    fun `keeps the shortcuts a command listed in the terminal history`() =
        runTest(mainDispatcherRule.dispatcher) {
            val shortcuts = RecordingCommand(
                id = Command.SHORTCUTS,
                result = CommandResult.Listing(
                    lines = listOf("mail publishes"),
                    shortcuts = listOf(inbox),
                ),
            )
            val viewModel = HomeViewModel(
                appRepository = FakeAppRepository(apps = listOf(mail)),
                preferencesRepository = RecordingPreferencesRepository(),
                batteryRepository = FakeBatteryRepository(status = null),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(shortcuts),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(viewModel)
            advanceUntilIdle()

            viewModel.type("shortcuts mail")
            advanceUntilIdle()
            viewModel.submitPrompt()
            advanceUntilIdle()

            assertEquals(
                listOf(
                    TerminalEntry(
                        id = 0L,
                        input = "shortcuts mail",
                        output = listOf("mail publishes"),
                        shortcuts = listOf(inbox),
                    ),
                ),
                viewModel.uiState.value.history,
            )
        }

    @Test
    fun `keeps the applications a command listed in the terminal history`() =
        runTest(mainDispatcherRule.dispatcher) {
            val pin = RecordingCommand(
                id = Command.PIN,
                result = CommandResult.Listing(
                    lines = listOf("mail matches more than one application"),
                    apps = listOf(mailbox, mailboxPro),
                ),
            )
            val viewModel = HomeViewModel(
                appRepository = FakeAppRepository(apps = listOf(mailbox, mailboxPro)),
                preferencesRepository = RecordingPreferencesRepository(),
                batteryRepository = FakeBatteryRepository(status = null),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(pin),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(viewModel)
            advanceUntilIdle()

            viewModel.type("pin mail")
            advanceUntilIdle()
            viewModel.submitPrompt()
            advanceUntilIdle()

            assertEquals(
                listOf(
                    TerminalEntry(
                        id = 0L,
                        input = "pin mail",
                        output = listOf("mail matches more than one application"),
                        apps = listOf(mailbox, mailboxPro),
                    ),
                ),
                viewModel.uiState.value.history,
            )
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
                batteryRepository = FakeBatteryRepository(status = null),
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
                batteryRepository = FakeBatteryRepository(status = null),
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
            viewModel.type("mail")
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
                batteryRepository = FakeBatteryRepository(status = null),
                launcherClock = FakeLauncherClock(),
                commandExecutor = commandExecutor(settings),
                packageMonitor = FakePackageMonitor(),
            )
            startCollecting(viewModel)
            advanceUntilIdle()
            val actions = collectSubmittedActions(viewModel)

            viewModel.type("settings")
            advanceUntilIdle()
            viewModel.submitPrompt()
            advanceUntilIdle()

            assertEquals(listOf(SubmittedAction.OpenSettings), actions)
            assertEquals(
                listOf(TerminalEntry(id = 0L, input = "settings", output = emptyList())),
                viewModel.uiState.value.history,
            )
            assertEquals("", viewModel.uiState.value.prompt.input)
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
        batteryRepository = FakeBatteryRepository(status = null),
        launcherClock = FakeLauncherClock(),
        commandExecutor = commandExecutor(),
        packageMonitor = FakePackageMonitor(),
    )

    private fun unixHomeState(
        apps: List<InstalledApp> = emptyList(),
        helpInvitation: String? = HELP_INVITATION,
    ): HomeUiState = HomeUiState(
        shellProfile = UnixShellProfile,
        shellContext = defaultShellContext(),
        apps = apps,
        helpInvitation = helpInvitation,
        statusClock = "22:10",
    )

    private fun defaultShellContext(): ShellContext = ShellContext(
        username = "user",
        hostname = "android",
        location = LauncherLocation.HOME,
    )

    private fun HomeViewModel.type(input: String) {
        val prompt = uiState.value.prompt
        updatePromptValue(
            prompt.copy(input = input, selection = TextRange(input.length), composition = null),
        )
    }

    private fun TestScope.startCollecting(viewModel: HomeViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
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

        override fun now(): Long = 1_700_000_000_000L
    }

}

/** The line an empty Home reads, which the Unix profile writes as it is. */
private const val HELP_INVITATION = "type help to list the commands"
