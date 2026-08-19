package com.gybra.terminallauncher.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gybra.terminallauncher.preferences.LauncherPreferences
import com.gybra.terminallauncher.preferences.PreferencesRepository
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
            update = { state -> state.copy(shellType = shellType) },
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

    public fun setUsername(username: String) {
        updateSetting(
            update = { state -> state.copy(username = username) },
            persist = { preferencesRepository.setUsername(username) },
        )
    }

    public fun setHostname(hostname: String) {
        updateSetting(
            update = { state -> state.copy(hostname = hostname) },
            persist = { preferencesRepository.setHostname(hostname) },
        )
    }

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
        shellType = shellType,
        terminalTheme = terminalTheme,
        showClock = showClock,
        username = username,
        hostname = hostname,
    )

    private companion object {
        const val STORAGE_ERROR = "Unable to save preferences"
    }
}
