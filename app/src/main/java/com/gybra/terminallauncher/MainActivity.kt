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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
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
import com.gybra.terminallauncher.launcher.SystemDeviceLock
import com.gybra.terminallauncher.launcher.PackageManagerAppRepository
import com.gybra.terminallauncher.launcher.SystemBatteryRepository
import com.gybra.terminallauncher.launcher.LauncherAppsShortcutRepository
import com.gybra.terminallauncher.launcher.ShortcutLauncher
import com.gybra.terminallauncher.launcher.SystemScreenLauncher
import com.gybra.terminallauncher.launcher.SystemTorch
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
    private var immersiveMode = true

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
        val systemScreenLauncher = SystemScreenLauncher(applicationContext)
        val shortcutLauncher = ShortcutLauncher(applicationContext)
        val batteryRepository = SystemBatteryRepository(applicationContext)
        val shortcutRepository = LauncherAppsShortcutRepository(applicationContext)
        val deviceLock = SystemDeviceLock(applicationContext)
        val commandExecutor = CommandExecutor(
            CommandRegistry(
                commands = launcherCommands(
                    preferencesRepository = preferencesRepository,
                    batteryRepository = batteryRepository,
                    shortcutRepository = shortcutRepository,
                    torch = SystemTorch(applicationContext),
                ),
            ),
        )
        val launcherViewModelFactory = viewModelFactory {
            initializer {
                HomeViewModel(
                    appRepository = appRepository,
                    preferencesRepository = preferencesRepository,
                    batteryRepository = batteryRepository,
                    launcherClock = launcherClock,
                    commandExecutor = commandExecutor,
                    packageMonitor = packageMonitor,
                )
            }
            initializer { SettingsViewModel(preferencesRepository, deviceLock) }
        }

        setLauncherContent(
            viewModelFactory = launcherViewModelFactory,
            appLauncher = appLauncher,
            shortcutLauncher = shortcutLauncher,
            systemScreenLauncher = systemScreenLauncher,
            deviceLock = deviceLock,
        )
    }

    private fun setLauncherContent(
        viewModelFactory: ViewModelProvider.Factory,
        appLauncher: AppLauncher,
        shortcutLauncher: ShortcutLauncher,
        systemScreenLauncher: SystemScreenLauncher,
        deviceLock: SystemDeviceLock,
    ) {
        setContent {
            val homeViewModel: HomeViewModel = viewModel(factory = viewModelFactory)
            val settingsViewModel: SettingsViewModel = viewModel(factory = viewModelFactory)
            val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
            val terminalColors = settingsState.terminalTheme.colors(isSystemInDarkTheme())
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                settingsViewModel.refreshDeviceLock()
            }
            SideEffect {
                updateSystemBarIconAppearance(
                    useDarkIcons = terminalColors.useDarkSystemBarIcons(),
                )
                applySystemBars(immersive = settingsState.immersiveMode)
            }
            LauncherApp(
                homeState = homeState,
                settingsState = settingsState,
                settingsActions = SettingsActions(
                    selectShell = settingsViewModel::selectShell,
                    selectTheme = settingsViewModel::selectTheme,
                    setShowClock = settingsViewModel::setShowClock,
                    setShowBattery = settingsViewModel::setShowBattery,
                    setImmersiveMode = settingsViewModel::setImmersiveMode,
                    setDoubleTapToLock = settingsViewModel::setDoubleTapToLock,
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
                    writeAppCommand = homeViewModel::writeAppCommand,
                    writeShortcutCommand = homeViewModel::writeShortcutCommand,
                ),
                submittedActions = homeViewModel.submittedActions,
                onLaunchApp = appLauncher::launch,
                onLaunchShortcut = shortcutLauncher::launch,
                onRowStart = homeViewModel::clearPrompt,
                onLockScreen = { deviceLock.lock() },
                onOpenSystemScreen = systemScreenLauncher::open,
                onRestartLauncher = ::recreate,
            )
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applySystemBars(immersive = immersiveMode)
        }
    }

    private fun configureFullScreenWindow() {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        WindowCompat.getInsetsController(window, window.decorView).systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        applySystemBars(immersive = immersiveMode)
    }

    /**
     * Hides the system bars, or gives them back, as the setting asks. The window answers focus
     * changes outside the composition, so the last applied value is kept here.
     */
    private fun applySystemBars(immersive: Boolean) {
        immersiveMode = immersive
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (immersive) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun updateSystemBarIconAppearance(useDarkIcons: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = useDarkIcons
            isAppearanceLightNavigationBars = useDarkIcons
        }
    }
}
