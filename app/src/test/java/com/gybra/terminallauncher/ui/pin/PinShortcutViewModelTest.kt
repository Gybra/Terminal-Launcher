package com.gybra.terminallauncher.ui.pin

import com.gybra.terminallauncher.MainDispatcherRule
import com.gybra.terminallauncher.launcher.FakeShortcutPinRequest
import com.gybra.terminallauncher.preferences.LauncherPreferences
import com.gybra.terminallauncher.preferences.RecordingPreferencesRepository
import com.gybra.terminallauncher.shell.ShellType
import com.gybra.terminallauncher.shell.dos.DosShellProfile
import com.gybra.terminallauncher.shell.unix.UnixShellProfile
import com.gybra.terminallauncher.theme.TerminalTheme
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PinShortcutViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val request = FakeShortcutPinRequest()

    @Test
    fun `shows the shortcut in the style of the stored shell`() = runTest {
        val preferencesRepository = RecordingPreferencesRepository(
            LauncherPreferences(shellType = ShellType.DOS, terminalTheme = TerminalTheme.AMBER),
        )
        val viewModel = PinShortcutViewModel(request, preferencesRepository)
        collectUiState(viewModel)
        advanceUntilIdle()

        assertEquals(
            PinShortcutUiState(
                shortcut = request.shortcut,
                shellProfile = DosShellProfile,
                terminalTheme = TerminalTheme.AMBER,
            ),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `shows the shortcut in the default shell until the preferences are read`() {
        val viewModel = PinShortcutViewModel(request, RecordingPreferencesRepository())

        assertEquals(
            PinShortcutUiState(
                shortcut = request.shortcut,
                shellProfile = UnixShellProfile,
                terminalTheme = TerminalTheme.SYSTEM,
            ),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `keeps the accepted shortcut on Home`() = runTest {
        val preferencesRepository = RecordingPreferencesRepository()
        val viewModel = PinShortcutViewModel(request, preferencesRepository)
        val answers = collectAnswers(viewModel)

        viewModel.accept()
        advanceUntilIdle()

        assertEquals(1, request.accepts)
        assertEquals(
            listOf("pinShortcut(org.example.browser, new-tab)"),
            preferencesRepository.writes,
        )
        assertEquals(listOf(request.shortcut), preferencesRepository.preferences.first().pinnedShortcuts)
        assertEquals(1, answers.size)
    }

    @Test
    fun `keeps nothing when Android refuses the request`() = runTest {
        val preferencesRepository = RecordingPreferencesRepository()
        val refused = FakeShortcutPinRequest(accepted = false)
        val viewModel = PinShortcutViewModel(refused, preferencesRepository)
        val answers = collectAnswers(viewModel)

        viewModel.accept()
        advanceUntilIdle()

        assertEquals(emptyList<String>(), preferencesRepository.writes)
        assertEquals(1, answers.size)
    }

    @Test
    fun `keeps nothing when the request is declined`() = runTest {
        val preferencesRepository = RecordingPreferencesRepository()
        val viewModel = PinShortcutViewModel(request, preferencesRepository)
        val answers = collectAnswers(viewModel)

        viewModel.decline()
        advanceUntilIdle()

        assertEquals(0, request.accepts)
        assertEquals(emptyList<String>(), preferencesRepository.writes)
        assertEquals(1, answers.size)
    }

    @Test
    fun `answers the request even when the shortcut cannot be stored`() = runTest {
        val preferencesRepository = RecordingPreferencesRepository(
            writeFailure = IOException("disk full"),
        )
        val viewModel = PinShortcutViewModel(request, preferencesRepository)
        val answers = collectAnswers(viewModel)

        viewModel.accept()
        advanceUntilIdle()

        assertEquals(1, answers.size)
    }

    private fun TestScope.collectAnswers(viewModel: PinShortcutViewModel): List<Unit> {
        val answers = mutableListOf<Unit>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.answered.collect { answer -> answers += answer }
        }
        return answers
    }

    private fun TestScope.collectUiState(viewModel: PinShortcutViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
    }
}
