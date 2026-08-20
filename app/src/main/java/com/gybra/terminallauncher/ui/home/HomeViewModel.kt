package com.gybra.terminallauncher.ui.home

import androidx.compose.ui.text.TextRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gybra.terminallauncher.command.CommandExecutor
import com.gybra.terminallauncher.command.CommandResult
import com.gybra.terminallauncher.launcher.AppRepository
import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.launcher.LauncherClock
import com.gybra.terminallauncher.preferences.LauncherPreferences
import com.gybra.terminallauncher.preferences.PreferencesRepository
import com.gybra.terminallauncher.search.AppSearchEngine
import com.gybra.terminallauncher.search.SearchResult
import com.gybra.terminallauncher.shell.LauncherLocation
import com.gybra.terminallauncher.shell.ShellContext
import com.gybra.terminallauncher.shell.ShellProfiles
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

public class HomeViewModel(
    private val appRepository: AppRepository,
    private val preferencesRepository: PreferencesRepository,
    private val launcherClock: LauncherClock,
    private val commandExecutor: CommandExecutor,
) : ViewModel() {
    private val initialPreferences = LauncherPreferences()
    private val promptState = MutableStateFlow(PromptState())
    private val history = MutableStateFlow<List<TerminalEntry>>(emptyList())
    private val installedApps: StateFlow<List<InstalledApp>> = appRepository
        .observeInstalledApps()
        .catch { failure ->
            if (failure is SecurityException) {
                emit(emptyList())
            } else {
                throw failure
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_TIMEOUT_MILLIS),
            initialValue = emptyList(),
        )

    public val uiState: StateFlow<HomeUiState> = combine(
        installedApps,
        preferencesRepository.preferences,
        launcherClock.observeTime(),
        promptState,
        history,
        ::createUiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_TIMEOUT_MILLIS),
        initialValue = createUiState(
            installedApps = emptyList(),
            preferences = initialPreferences,
            clockText = "",
            prompt = PromptState(),
            history = emptyList(),
        ),
    )

    public fun updatePromptValue(value: PromptState) {
        promptState.update { state ->
            value.copy(focused = state.focused)
        }
    }

    public fun updatePromptFocus(focused: Boolean) {
        promptState.update { state -> state.copy(focused = focused) }
    }

    /**
     * Runs the submitted input as a registered command, and otherwise returns the application to
     * launch when the query resolves to a single match. Consumed input joins the terminal history
     * and clears the prompt, so only ambiguous queries and their results stay on screen.
     */
    public fun submitPrompt(): InstalledApp? {
        val state = uiState.value
        val submittedInput = state.prompt.input
        val result = commandExecutor.execute(
            input = submittedInput,
            shellProfile = state.shellProfile,
            installedApps = installedApps.value,
        )
        return when (result) {
            is CommandResult.Output -> {
                recordEntry(input = submittedInput, output = result.lines)
                null
            }
            CommandResult.ClearHistory -> {
                eraseHistory()
                null
            }
            CommandResult.Search -> resolveSearchedApp(submittedInput, state.searchResults)
        }
    }

    private fun resolveSearchedApp(
        submittedInput: String,
        searchResults: List<SearchResult>,
    ): InstalledApp? {
        val resolvedApp = AppSearchEngine.unambiguousMatch(searchResults) ?: return null
        recordEntry(input = submittedInput, output = emptyList())
        return resolvedApp
    }

    private fun recordEntry(input: String, output: List<String>) {
        history.update { entries ->
            val entry = TerminalEntry(
                id = (entries.lastOrNull()?.id ?: -1L) + 1L,
                input = input,
                output = output,
            )
            (entries + entry).takeLast(MAX_HISTORY_ENTRIES)
        }
        clearPrompt()
    }

    private fun eraseHistory() {
        history.value = emptyList()
        clearPrompt()
    }

    private fun clearPrompt() {
        promptState.update { state ->
            state.copy(input = "", selection = TextRange.Zero, composition = null)
        }
    }

    private fun createUiState(
        installedApps: List<InstalledApp>,
        preferences: LauncherPreferences,
        clockText: String,
        prompt: PromptState,
        history: List<TerminalEntry>,
    ): HomeUiState = HomeUiState(
        shellProfile = ShellProfiles.forType(preferences.shellType),
        shellContext = preferences.toShellContext(),
        apps = installedApps.filter { app -> app.packageName in preferences.pinnedPackages },
        searchResults = AppSearchEngine.search(query = prompt.input, apps = installedApps),
        history = history,
        clockText = clockText.takeIf { preferences.showClock && it.isNotEmpty() },
        prompt = prompt,
    )

    private fun LauncherPreferences.toShellContext(): ShellContext = ShellContext(
        username = username,
        hostname = hostname,
        location = LauncherLocation.HOME,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val MAX_HISTORY_ENTRIES = 20
    }
}
