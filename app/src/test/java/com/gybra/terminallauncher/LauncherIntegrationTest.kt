package com.gybra.terminallauncher

import com.gybra.terminallauncher.command.CommandExecutor
import com.gybra.terminallauncher.command.CommandRegistry
import com.gybra.terminallauncher.command.launcherCommands
import com.gybra.terminallauncher.launcher.FakeAppRepository
import com.gybra.terminallauncher.launcher.FakeLauncherClock
import com.gybra.terminallauncher.launcher.FakePackageMonitor
import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.preferences.LauncherPreferences
import com.gybra.terminallauncher.preferences.RecordingPreferencesRepository
import com.gybra.terminallauncher.shell.ShellType
import com.gybra.terminallauncher.ui.home.HomeViewModel
import com.gybra.terminallauncher.ui.home.PromptState
import com.gybra.terminallauncher.ui.home.SubmittedAction
import com.gybra.terminallauncher.ui.home.TerminalEntry
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Drives Home with the command set the launcher actually registers, so the prompt, the command
 * engine, the search engine, the shell profiles, and the stored preferences meet end to end.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LauncherIntegrationTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `lists installed applications the way the selected shell writes them`() =
        runTest(mainDispatcherRule.dispatcher) {
            val unixHome = launcherHome()
            unixHome.submit("ls")

            assertEquals(
                listOf(TerminalEntry(id = 0L, input = "ls", output = listOf("camera", "mail"))),
                unixHome.viewModel.uiState.value.history,
            )

            val dosHome = launcherHome(
                preferences = LauncherPreferences(shellType = ShellType.DOS),
            )
            dosHome.submit("DIR")

            assertEquals(
                listOf(
                    TerminalEntry(
                        id = 0L,
                        input = "DIR",
                        output = listOf("CAMERA.EXE", "MAIL.EXE", "", "2 File(s)"),
                    ),
                ),
                dosHome.viewModel.uiState.value.history,
            )
        }

    @Test
    fun `pins and unpins an application from the prompt`() =
        runTest(mainDispatcherRule.dispatcher) {
            val home = launcherHome()

            home.submit("pin camera")

            assertEquals(listOf(camera), home.viewModel.uiState.value.apps)
            assertEquals(listOf("pinPackage(org.example.camera)"), home.preferences.writes)

            home.submit("unpin camera")

            assertEquals(emptyList<InstalledApp>(), home.viewModel.uiState.value.apps)
            assertEquals(
                listOf("pinPackage(org.example.camera)", "unpinPackage(org.example.camera)"),
                home.preferences.writes,
            )
        }

    @Test
    fun `describes every registered command through help`() =
        runTest(mainDispatcherRule.dispatcher) {
            val home = launcherHome()

            home.submit("help")

            assertEquals(
                listOf(
                    "ls        List installed applications",
                    "help      Show available commands",
                    "pin       Pin an application to Home",
                    "unpin     Remove an application from Home",
                    "clear     Clear the terminal history",
                    "settings  Open the launcher settings",
                ),
                home.viewModel.uiState.value.history.single().output,
            )
        }

    @Test
    fun `clears the terminal history`() = runTest(mainDispatcherRule.dispatcher) {
        val home = launcherHome()
        home.submit("ls")
        home.submit("help")
        assertEquals(2, home.viewModel.uiState.value.history.size)

        home.submit("clear")

        assertEquals(emptyList<TerminalEntry>(), home.viewModel.uiState.value.history)
    }

    @Test
    fun `launches an application typed at the prompt`() =
        runTest(mainDispatcherRule.dispatcher) {
            val home = launcherHome()

            home.submit("camera")

            assertEquals(listOf(SubmittedAction.LaunchApp(camera)), home.actions)
            assertEquals(
                listOf(TerminalEntry(id = 0L, input = "camera", output = emptyList())),
                home.viewModel.uiState.value.history,
            )
        }

    @Test
    fun `opens settings from the prompt`() = runTest(mainDispatcherRule.dispatcher) {
        val home = launcherHome()

        home.submit("settings")

        assertEquals(listOf(SubmittedAction.OpenSettings), home.actions)
    }

    @Test
    fun `reports a command that cannot reach storage instead of crashing`() =
        runTest(mainDispatcherRule.dispatcher) {
            val home = launcherHome(writeFailure = IOException("storage unavailable"))

            home.submit("pin camera")

            assertEquals(
                listOf(TerminalEntry(id = 0L, input = "pin camera", output = listOf("pin failed"))),
                home.viewModel.uiState.value.history,
            )

            home.submit("ls")

            assertEquals(
                listOf("camera", "mail"),
                home.viewModel.uiState.value.history.last().output,
            )
        }

    private fun TestScope.launcherHome(
        preferences: LauncherPreferences = LauncherPreferences(),
        writeFailure: IOException? = null,
    ): LauncherHome {
        val preferencesRepository = RecordingPreferencesRepository(
            initialPreferences = preferences,
            writeFailure = writeFailure,
        )
        val viewModel = HomeViewModel(
            appRepository = FakeAppRepository(apps = installedApps),
            preferencesRepository = preferencesRepository,
            launcherClock = FakeLauncherClock(),
            commandExecutor = CommandExecutor(
                CommandRegistry(commands = launcherCommands(preferencesRepository)),
            ),
            packageMonitor = FakePackageMonitor(),
        )
        val actions = mutableListOf<SubmittedAction>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.submittedActions.collect { action -> actions += action }
        }
        advanceUntilIdle()
        return LauncherHome(this, viewModel, preferencesRepository, actions)
    }

    private class LauncherHome(
        private val scope: TestScope,
        val viewModel: HomeViewModel,
        val preferences: RecordingPreferencesRepository,
        val actions: List<SubmittedAction>,
    ) {
        fun submit(input: String) {
            viewModel.updatePromptValue(PromptState(input = input))
            scope.advanceUntilIdle()
            viewModel.submitPrompt()
            scope.advanceUntilIdle()
        }
    }

    private val camera = InstalledApp(packageName = "org.example.camera", label = "Camera")
    private val mail = InstalledApp(packageName = "org.example.mail", label = "Mail")
    private val installedApps = listOf(camera, mail)
}
