package com.gybra.terminallauncher.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gybra.terminallauncher.preferences.LauncherPreferences
import com.gybra.terminallauncher.preferences.PreferencesRepository
import com.gybra.terminallauncher.shell.ShellType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

public class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(LauncherPreferences().toUiState())
    public val uiState: StateFlow<SettingsUiState> = mutableUiState.asStateFlow()

    init {
        observePreferences()
    }

    public fun selectShell(shellType: ShellType) {
        viewModelScope.launch { preferencesRepository.setShellType(shellType) }
    }

    public fun setShowClock(showClock: Boolean) {
        viewModelScope.launch { preferencesRepository.setShowClock(showClock) }
    }

    public fun setUsername(username: String) {
        viewModelScope.launch { preferencesRepository.setUsername(username) }
    }

    public fun setHostname(hostname: String) {
        viewModelScope.launch { preferencesRepository.setHostname(hostname) }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesRepository.preferences.collect { preferences ->
                mutableUiState.value = preferences.toUiState()
            }
        }
    }

    private fun LauncherPreferences.toUiState(): SettingsUiState = SettingsUiState(
        shellType = shellType,
        showClock = showClock,
        username = username,
        hostname = hostname,
    )
}
