package com.gybra.terminallauncher.ui.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gybra.terminallauncher.launcher.ShortcutPinRequest
import com.gybra.terminallauncher.preferences.LauncherPreferences
import com.gybra.terminallauncher.preferences.PreferencesRepository
import com.gybra.terminallauncher.shell.ShellProfiles
import java.io.IOException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Answers one request from an application to pin a shortcut to Home. Nothing is pinned until the
 * user accepts, and the shortcut is remembered only once Android confirms the answer.
 */
public class PinShortcutViewModel(
    private val request: ShortcutPinRequest,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {
    private val answers = Channel<Unit>(capacity = Channel.BUFFERED)

    /** Emits once the request is answered, so the confirmation can leave the screen. */
    public val answered: Flow<Unit> = answers.receiveAsFlow()

    public val uiState: StateFlow<PinShortcutUiState> = preferencesRepository.preferences
        .map(::toUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_TIMEOUT_MILLIS),
            initialValue = toUiState(LauncherPreferences()),
        )

    /** Pins the shortcut Android is holding, unless Android has already withdrawn the request. */
    public fun accept() {
        viewModelScope.launch {
            if (request.accept()) {
                pinQuietly()
            }
            answers.send(Unit)
        }
    }

    /** Leaves the request unanswered, which is how Android learns the shortcut is refused. */
    public fun decline() {
        viewModelScope.launch { answers.send(Unit) }
    }

    private suspend fun pinQuietly() {
        try {
            preferencesRepository.pinShortcut(request.shortcut)
        } catch (_: IOException) {
            // Android pinned the shortcut either way, so a failed write only costs Home a row.
        }
    }

    private fun toUiState(preferences: LauncherPreferences): PinShortcutUiState = PinShortcutUiState(
        shortcut = request.shortcut,
        shellProfile = ShellProfiles.forType(preferences.shellType),
        terminalTheme = preferences.terminalTheme,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
