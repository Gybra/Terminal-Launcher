package com.gybra.terminallauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gybra.terminallauncher.launcher.ShortcutPinRequest
import com.gybra.terminallauncher.launcher.ShortcutPinRequests
import com.gybra.terminallauncher.preferences.DataStorePreferencesRepository
import com.gybra.terminallauncher.preferences.launcherDataStore
import com.gybra.terminallauncher.ui.pin.PinShortcutScreen
import com.gybra.terminallauncher.ui.pin.PinShortcutViewModel
import com.gybra.terminallauncher.ui.theme.TerminalThemeProvider

/**
 * Asks whether the shortcut an application offers may stay on Home. Android starts this activity
 * for `CONFIRM_PIN_SHORTCUT`, and an intent carrying anything else is left unanswered.
 */
public class PinShortcutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val request = ShortcutPinRequests(applicationContext).read(intent)
        if (request == null) {
            finish()
            return
        }

        setConfirmationContent(request)
    }

    private fun setConfirmationContent(request: ShortcutPinRequest) {
        val preferencesRepository = DataStorePreferencesRepository(
            applicationContext.launcherDataStore,
        )
        val pinViewModelFactory = viewModelFactory {
            initializer { PinShortcutViewModel(request, preferencesRepository) }
        }

        setContent {
            val pinViewModel: PinShortcutViewModel = viewModel(factory = pinViewModelFactory)
            val state by pinViewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(pinViewModel) { pinViewModel.answered.collect { finish() } }
            TerminalThemeProvider(theme = state.terminalTheme) {
                PinShortcutScreen(
                    state = state,
                    onAccept = pinViewModel::accept,
                    onDecline = pinViewModel::decline,
                )
            }
        }
    }
}
