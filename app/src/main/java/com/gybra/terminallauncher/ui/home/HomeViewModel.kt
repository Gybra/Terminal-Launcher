package com.gybra.terminallauncher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gybra.terminallauncher.launcher.AppRepository
import com.gybra.terminallauncher.launcher.LauncherClock
import com.gybra.terminallauncher.preferences.LauncherPreferences
import com.gybra.terminallauncher.preferences.PreferencesRepository
import com.gybra.terminallauncher.shell.ShellProfiles
import com.gybra.terminallauncher.shell.LauncherLocation
import com.gybra.terminallauncher.shell.ShellContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

public class HomeViewModel(
    private val appRepository: AppRepository,
    private val preferencesRepository: PreferencesRepository,
    private val launcherClock: LauncherClock,
) : ViewModel() {
    private val initialPreferences = LauncherPreferences()
    private val mutableUiState = MutableStateFlow(
        HomeUiState(
            shellProfile = ShellProfiles.forType(initialPreferences.shellType),
            shellContext = initialPreferences.toShellContext(),
        ),
    )
    public val uiState: StateFlow<HomeUiState> = mutableUiState.asStateFlow()

    init {
        observeHomeState()
    }

    private fun observeHomeState() {
        viewModelScope.launch {
            val apps = appRepository
                .observeInstalledApps()
                .catch { failure ->
                    if (failure is SecurityException) {
                        emit(emptyList())
                    } else {
                        throw failure
                    }
                }

            combine(
                apps,
                preferencesRepository.preferences,
                launcherClock.observeTime(),
            ) { installedApps, preferences, clockText ->
                    HomeUiState(
                        shellProfile = ShellProfiles.forType(preferences.shellType),
                        shellContext = preferences.toShellContext(),
                        apps = installedApps.filter { app ->
                            app.packageName in preferences.pinnedPackages
                        },
                        clockText = clockText.takeIf { preferences.showClock },
                    )
                }
                .collect(mutableUiState)
        }
    }

    private fun LauncherPreferences.toShellContext(): ShellContext = ShellContext(
        username = username,
        hostname = hostname,
        location = LauncherLocation.HOME,
    )
}
