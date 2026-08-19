package com.gybra.terminallauncher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gybra.terminallauncher.launcher.AppRepository
import com.gybra.terminallauncher.preferences.LauncherPreferences
import com.gybra.terminallauncher.preferences.PreferencesRepository
import com.gybra.terminallauncher.shell.ShellProfiles
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

public class HomeViewModel(
    private val appRepository: AppRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(
        HomeUiState(
            shellProfile = ShellProfiles.forType(LauncherPreferences().shellType),
        ),
    )
    public val uiState: StateFlow<HomeUiState> = mutableUiState.asStateFlow()

    init {
        observeHomeState()
    }

    private fun observeHomeState() {
        viewModelScope.launch {
            appRepository
                .observeInstalledApps()
                .catch { failure ->
                    if (failure is SecurityException) {
                        emit(emptyList())
                    } else {
                        throw failure
                    }
                }
                .combine(preferencesRepository.preferences) { apps, preferences ->
                    HomeUiState(
                        apps = apps.filter { app ->
                            app.packageName in preferences.pinnedPackages
                        },
                        shellProfile = ShellProfiles.forType(preferences.shellType),
                    )
                }
                .collect(mutableUiState)
        }
    }
}
