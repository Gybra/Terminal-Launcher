package com.gybra.terminallauncher.ui.home

import androidx.compose.ui.text.TextRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gybra.terminallauncher.command.CommandExecutor
import com.gybra.terminallauncher.command.CommandResult
import com.gybra.terminallauncher.launcher.AppRepository
import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.launcher.LauncherClock
import com.gybra.terminallauncher.launcher.PackageMonitor
import com.gybra.terminallauncher.preferences.LauncherPreferences
import com.gybra.terminallauncher.preferences.PreferencesRepository
import com.gybra.terminallauncher.search.AppSearchEngine
import com.gybra.terminallauncher.search.SearchResult
import com.gybra.terminallauncher.shell.LauncherLocation
import com.gybra.terminallauncher.shell.ShellContext
import com.gybra.terminallauncher.shell.ShellProfiles
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

public class HomeViewModel(
    private val appRepository: AppRepository,
    private val preferencesRepository: PreferencesRepository,
    private val launcherClock: LauncherClock,
    private val commandExecutor: CommandExecutor,
    private val packageMonitor: PackageMonitor,
) : ViewModel() {
    private val initialPreferences = LauncherPreferences()
    private val promptState = MutableStateFlow(PromptState())
    private val history = MutableStateFlow<List<TerminalEntry>>(emptyList())
    private val submittedActionRequests = Channel<SubmittedAction>(capacity = Channel.BUFFERED)

    /** Actions the composition root performs after a submitted line, each delivered once. */
    public val submittedActions: Flow<SubmittedAction> = submittedActionRequests.receiveAsFlow()
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
    private val preferences: StateFlow<LauncherPreferences> = preferencesRepository.preferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_TIMEOUT_MILLIS),
            initialValue = initialPreferences,
        )

    public val uiState: StateFlow<HomeUiState> = combine(
        installedApps,
        preferences,
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

    init {
        unpinRemovedPackages()
    }

    public fun updatePromptValue(value: PromptState) {
        promptState.update { state ->
            value.copy(focused = state.focused)
        }
    }

    public fun updatePromptFocus(focused: Boolean) {
        promptState.update { state -> state.copy(focused = focused) }
    }

    /**
     * Runs the submitted input as a registered command, and otherwise searches it among the
     * installed applications. Consumed input joins the terminal history and clears the prompt, so
     * only ambiguous queries and their results stay on screen.
     */
    public fun submitPrompt() {
        viewModelScope.launch {
            val state = uiState.value
            val submittedInput = state.prompt.input
            val result = commandExecutor.execute(
                input = submittedInput,
                shellProfile = state.shellProfile,
                installedApps = installedApps.value,
            )
            when (result) {
                is CommandResult.Output -> recordEntry(submittedInput, output = result.lines)
                CommandResult.ClearHistory -> eraseHistory()
                CommandResult.OpenSettings ->
                    completeSubmission(submittedInput, SubmittedAction.OpenSettings)
                is CommandResult.OpenSystemScreen ->
                    completeSubmission(
                        submittedInput,
                        SubmittedAction.OpenSystemScreen(result.screen),
                    )
                CommandResult.RestartLauncher ->
                    completeSubmission(submittedInput, SubmittedAction.RestartLauncher)
                CommandResult.Search -> launchSubmittedApp(submittedInput, state.searchResults)
            }
        }
    }

    private suspend fun launchSubmittedApp(
        submittedInput: String,
        searchResults: List<SearchResult>,
    ) {
        val resolvedApp = aliasedApp(submittedInput)
            ?: AppSearchEngine.unambiguousMatch(searchResults)
            ?: return
        recordLaunch(resolvedApp.packageName)
        completeSubmission(submittedInput, SubmittedAction.LaunchApp(resolvedApp))
    }

    private suspend fun recordLaunch(packageName: String) {
        try {
            preferencesRepository.recordLaunch(packageName, launchedAt = launcherClock.now())
        } catch (_: IOException) {
            // A lost launch only costs the application some rank, so it must not stop the launch.
        }
    }

    /**
     * Returns the application named by an alias. Registered commands are resolved before this, so
     * an alias can never take over a command, and an alias left pointing at an application that is
     * no longer installed simply falls back to search.
     */
    private fun aliasedApp(submittedInput: String): InstalledApp? {
        val aliasedPackage = preferences.value.aliases[submittedInput.trim().lowercase(Locale.ROOT)]
            ?: return null

        return installedApps.value.firstOrNull { app -> app.packageName == aliasedPackage }
    }

    private suspend fun completeSubmission(submittedInput: String, action: SubmittedAction) {
        recordEntry(submittedInput, output = emptyList())
        submittedActionRequests.send(action)
    }

    private fun unpinRemovedPackages() {
        viewModelScope.launch {
            packageMonitor.observeChanges()
                .filter { change -> change.removed }
                .collect { change -> unpinQuietly(change.packageName) }
        }
    }

    private suspend fun unpinQuietly(packageName: String) {
        try {
            preferencesRepository.unpinPackage(packageName)
            preferencesRepository.unpinShortcuts(packageName)
        } catch (_: IOException) {
            // Home already hides packages that are not installed, so a failed write only leaves a
            // stale entry in storage.
        }
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
        shortcuts = preferences.pinnedShortcuts,
        searchResults = AppSearchEngine.search(
            query = prompt.input,
            apps = installedApps,
            usage = preferences.usage,
            pinnedPackages = preferences.pinnedPackages,
        ),
        history = history,
        clockText = clockText.takeIf { preferences.showClock && it.isNotEmpty() },
        prompt = prompt,
    )

    private fun LauncherPreferences.toShellContext(): ShellContext = ShellContext(
        username = username,
        hostname = hostname,
        location = LauncherLocation.HOME,
        promptSymbol = promptSymbol,
        showPath = showPromptPath,
        dosDrive = dosDrive,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val MAX_HISTORY_ENTRIES = 20
    }
}
