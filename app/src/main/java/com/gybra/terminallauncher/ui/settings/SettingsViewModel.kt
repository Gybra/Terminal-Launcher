package com.gybra.terminallauncher.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gybra.terminallauncher.launcher.DeviceLock
import com.gybra.terminallauncher.preferences.LauncherPreferences
import com.gybra.terminallauncher.preferences.PreferencesRepository
import com.gybra.terminallauncher.shell.DosDrive
import com.gybra.terminallauncher.shell.PromptSymbol
import com.gybra.terminallauncher.shell.ShellProfiles
import com.gybra.terminallauncher.shell.ShellType
import com.gybra.terminallauncher.theme.TerminalTheme
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

public class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val deviceLock: DeviceLock,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(LauncherPreferences().toUiState())
    private val writeMutex = Mutex()
    private var pendingWrites = 0
    private var batchFailed = false
    public val uiState: StateFlow<SettingsUiState> = mutableUiState.asStateFlow()

    init {
        observePreferences()
    }

    public fun selectShell(shellType: ShellType) {
        updateSetting(
            update = { state -> state.copy(shellProfile = ShellProfiles.forType(shellType)) },
            persist = { preferencesRepository.setShellType(shellType) },
        )
    }

    public fun selectTheme(terminalTheme: TerminalTheme) {
        updateSetting(
            update = { state -> state.copy(terminalTheme = terminalTheme) },
            persist = { preferencesRepository.setTerminalTheme(terminalTheme) },
        )
    }

    public fun setShowClock(showClock: Boolean) {
        updateSetting(
            update = { state -> state.copy(showClock = showClock) },
            persist = { preferencesRepository.setShowClock(showClock) },
        )
    }

    public fun setShowBattery(showBattery: Boolean) {
        updateSetting(
            update = { state -> state.copy(showBattery = showBattery) },
            persist = { preferencesRepository.setShowBattery(showBattery) },
        )
    }

    public fun setImmersiveMode(immersiveMode: Boolean) {
        updateSetting(
            update = { state -> state.copy(immersiveMode = immersiveMode) },
            persist = { preferencesRepository.setImmersiveMode(immersiveMode) },
        )
    }

    /**
     * Sends the user where the accessibility service that locking the screen needs is turned
     * on, or turns it off. The toggle follows Android, so a service left off leaves it off.
     */
    public fun setDoubleTapToLock(enabled: Boolean) {
        if (enabled) {
            deviceLock.requestEnable()
        } else {
            deviceLock.disable()
        }
        refreshDeviceLock()
    }

    /** Reads the service again, such as after the user came back from the Android settings. */
    public fun refreshDeviceLock() {
        mutableUiState.value = mutableUiState.value.copy(doubleTapToLock = deviceLock.enabled)
    }

    /** Stores [username] as a prompt token, so what is typed can always be written in a prompt. */
    public fun setUsername(username: String) {
        val token = username.asPromptToken()
        updateSetting(
            update = { state -> state.copy(username = token) },
            persist = { preferencesRepository.setUsername(token) },
        )
    }

    /** Stores [hostname] as a prompt token, under the same rule as the username. */
    public fun setHostname(hostname: String) {
        val token = hostname.asPromptToken()
        updateSetting(
            update = { state -> state.copy(hostname = token) },
            persist = { preferencesRepository.setHostname(token) },
        )
    }

    public fun selectPromptSymbol(promptSymbol: PromptSymbol) {
        updateSetting(
            update = { state -> state.copy(promptSymbol = promptSymbol) },
            persist = { preferencesRepository.setPromptSymbol(promptSymbol) },
        )
    }

    public fun setShowPromptPath(showPromptPath: Boolean) {
        updateSetting(
            update = { state -> state.copy(showPromptPath = showPromptPath) },
            persist = { preferencesRepository.setShowPromptPath(showPromptPath) },
        )
    }

    public fun selectDosDrive(dosDrive: DosDrive) {
        updateSetting(
            update = { state -> state.copy(dosDrive = dosDrive) },
            persist = { preferencesRepository.setDosDrive(dosDrive) },
        )
    }

    /**
     * Keeps an identity usable in a prompt: whitespace and control characters never reach it, and
     * it stays short enough to leave the rest of the line readable.
     */
    private fun String.asPromptToken(): String =
        filterNot { character -> character.isWhitespace() || character.isISOControl() }
            .take(MAX_PROMPT_TOKEN_LENGTH)

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesRepository.preferences.collect { preferences ->
                if (pendingWrites == 0) {
                    mutableUiState.value = preferences.toUiState()
                }
            }
        }
    }

    private fun updateSetting(
        update: (SettingsUiState) -> SettingsUiState,
        persist: suspend () -> Unit,
    ) {
        mutableUiState.value = update(mutableUiState.value).copy(storageError = null)
        pendingWrites += 1
        viewModelScope.launch {
            try {
                writeMutex.withLock { persist() }
            } catch (_: IOException) {
                batchFailed = true
            }

            pendingWrites -= 1
            if (pendingWrites == 0) {
                refreshPersistedState()
            }
        }
    }

    private suspend fun refreshPersistedState() {
        val preferences = preferencesRepository.preferences.first()
        if (pendingWrites == 0) {
            val error = if (batchFailed) STORAGE_ERROR else null
            batchFailed = false
            mutableUiState.value = preferences.toUiState().copy(storageError = error)
        }
    }

    private fun LauncherPreferences.toUiState(): SettingsUiState = SettingsUiState(
        shellProfile = ShellProfiles.forType(shellType),
        terminalTheme = terminalTheme,
        showClock = showClock,
        showBattery = showBattery,
        immersiveMode = immersiveMode,
        doubleTapToLock = deviceLock.enabled,
        username = username,
        hostname = hostname,
        promptSymbol = promptSymbol,
        showPromptPath = showPromptPath,
        dosDrive = dosDrive,
    )

    private companion object {
        const val STORAGE_ERROR = "Unable to save preferences"
        const val MAX_PROMPT_TOKEN_LENGTH = 16
    }
}
