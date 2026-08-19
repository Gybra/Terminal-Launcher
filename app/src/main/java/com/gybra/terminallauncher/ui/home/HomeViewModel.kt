package com.gybra.terminallauncher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gybra.terminallauncher.launcher.AppRepository
import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.launcher.LauncherClock
import com.gybra.terminallauncher.preferences.LauncherPreferences
import com.gybra.terminallauncher.preferences.PreferencesRepository
import com.gybra.terminallauncher.shell.ShellProfiles
import com.gybra.terminallauncher.shell.LauncherLocation
import com.gybra.terminallauncher.shell.ShellContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

public class HomeViewModel(
    private val appRepository: AppRepository,
    private val preferencesRepository: PreferencesRepository,
    private val launcherClock: LauncherClock,
) : ViewModel() {
    private val initialPreferences = LauncherPreferences()
    private val installedApps = appRepository
        .observeInstalledApps()
        .catch { failure ->
            if (failure is SecurityException) {
                emit(emptyList())
            } else {
                throw failure
            }
        }

    public val uiState: StateFlow<HomeUiState> = combine(
        installedApps,
        preferencesRepository.preferences,
        launcherClock.observeTime(),
        ::createUiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
        initialValue = createUiState(
            installedApps = emptyList(),
            preferences = initialPreferences,
            clockText = "",
        ),
    )

    private fun createUiState(
        installedApps: List<InstalledApp>,
        preferences: LauncherPreferences,
        clockText: String,
    ): HomeUiState = HomeUiState(
        shellProfile = ShellProfiles.forType(preferences.shellType),
        shellContext = preferences.toShellContext(),
        apps = installedApps.filter { app -> app.packageName in preferences.pinnedPackages },
        clockText = clockText.takeIf { preferences.showClock && it.isNotEmpty() },
    )

    private fun LauncherPreferences.toShellContext(): ShellContext = ShellContext(
        username = username,
        hostname = hostname,
        location = LauncherLocation.HOME,
    )
}
