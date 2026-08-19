package com.gybra.terminallauncher

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gybra.terminallauncher.launcher.AppLauncher
import com.gybra.terminallauncher.launcher.PackageManagerAppRepository
import com.gybra.terminallauncher.preferences.DataStorePreferencesRepository
import com.gybra.terminallauncher.preferences.launcherDataStore
import com.gybra.terminallauncher.ui.home.HomeScreen
import com.gybra.terminallauncher.ui.home.HomeViewModel

public class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureFullScreenWindow()

        val appRepository = PackageManagerAppRepository(
            packageManager = packageManager,
            launcherPackageName = packageName,
        )
        val preferencesRepository = DataStorePreferencesRepository(applicationContext.launcherDataStore)
        val appLauncher = AppLauncher(applicationContext)
        val homeViewModelFactory = viewModelFactory {
            initializer {
                HomeViewModel(
                    appRepository = appRepository,
                    preferencesRepository = preferencesRepository,
                )
            }
        }

        setContent {
            val homeViewModel: HomeViewModel = viewModel(factory = homeViewModelFactory)
            val state by homeViewModel.uiState.collectAsStateWithLifecycle()

            HomeScreen(
                state = state,
                onAppClick = appLauncher::launch,
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
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
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
}
