package com.gybra.terminallauncher

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gybra.terminallauncher.command.CommandExecutor
import com.gybra.terminallauncher.command.CommandRegistry
import com.gybra.terminallauncher.command.launcherCommands
import com.gybra.terminallauncher.launcher.AppLauncher
import com.gybra.terminallauncher.launcher.BroadcastPackageMonitor
import com.gybra.terminallauncher.launcher.PackageManagerAppRepository
import com.gybra.terminallauncher.launcher.SystemLauncherClock
import com.gybra.terminallauncher.preferences.DataStorePreferencesRepository
import com.gybra.terminallauncher.preferences.launcherDataStore
import com.gybra.terminallauncher.theme.colors
import com.gybra.terminallauncher.theme.useDarkSystemBarIcons
import com.gybra.terminallauncher.ui.LauncherApp
import com.gybra.terminallauncher.ui.home.HomeViewModel
import com.gybra.terminallauncher.ui.home.PromptActions
import com.gybra.terminallauncher.ui.settings.SettingsActions
import com.gybra.terminallauncher.ui.settings.SettingsViewModel

public class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureFullScreenWindow()

        val packageMonitor = BroadcastPackageMonitor(applicationContext)
        val appRepository = PackageManagerAppRepository(
            packageManager = packageManager,
            launcherPackageName = packageName,
            packageMonitor = packageMonitor,
        )
        val preferencesRepository = DataStorePreferencesRepository(applicationContext.launcherDataStore)
        val appLauncher = AppLauncher(applicationContext)
        val launcherClock = SystemLauncherClock()
        val commandExecutor = CommandExecutor(
            CommandRegistry(commands = launcherCommands(preferencesRepository)),
        )
        val launcherViewModelFactory = viewModelFactory {
            initializer {
                HomeViewModel(
                    appRepository = appRepository,
                    preferencesRepository = preferencesRepository,
                    launcherClock = launcherClock,
                    commandExecutor = commandExecutor,
                    packageMonitor = packageMonitor,
                )
            }
            initializer { SettingsViewModel(preferencesRepository) }
        }

        setLauncherContent(
            viewModelFactory = launcherViewModelFactory,
            appLauncher = appLauncher,
        )
    }

    private fun setLauncherContent(
        viewModelFactory: ViewModelProvider.Factory,
        appLauncher: AppLauncher,
    ) {
        setContent {
            val homeViewModel: HomeViewModel = viewModel(factory = viewModelFactory)
            val settingsViewModel: SettingsViewModel = viewModel(factory = viewModelFactory)
            val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
            val terminalColors = settingsState.terminalTheme.colors(isSystemInDarkTheme())
            SideEffect {
                updateSystemBarIconAppearance(
                    useDarkIcons = terminalColors.useDarkSystemBarIcons(),
                )
            }
            LauncherApp(
                homeState = homeState,
                settingsState = settingsState,
                settingsActions = SettingsActions(
                    selectShell = settingsViewModel::selectShell,
                    selectTheme = settingsViewModel::selectTheme,
                    setShowClock = settingsViewModel::setShowClock,
                    setUsername = settingsViewModel::setUsername,
                    setHostname = settingsViewModel::setHostname,
                    selectPromptSymbol = settingsViewModel::selectPromptSymbol,
                    setShowPromptPath = settingsViewModel::setShowPromptPath,
                    selectDosDrive = settingsViewModel::selectDosDrive,
                ),
                promptActions = PromptActions(
                    updateValue = homeViewModel::updatePromptValue,
                    updateFocus = homeViewModel::updatePromptFocus,
                    submit = homeViewModel::submitPrompt,
                ),
                submittedActions = homeViewModel.submittedActions,
                onLaunchApp = appLauncher::launch,
            )
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun configureFullScreenWindow() {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        WindowCompat.getInsetsController(window, window.decorView).systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat
            .getInsetsController(window, window.decorView)
            .hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun updateSystemBarIconAppearance(useDarkIcons: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = useDarkIcons
            isAppearanceLightNavigationBars = useDarkIcons
        }
    }
}
