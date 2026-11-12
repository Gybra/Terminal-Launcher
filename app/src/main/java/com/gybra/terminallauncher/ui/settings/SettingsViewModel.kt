package com.gybra.terminallauncher.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gybra.terminallauncher.launcher.DeviceLock
import com.gybra.terminallauncher.preferences.AccessibilityConsent
import com.gybra.terminallauncher.preferences.LauncherPreferences
import com.gybra.terminallauncher.preferences.PreferencesRepository
import com.gybra.terminallauncher.shell.DosDrive
import com.gybra.terminallauncher.shell.PromptSymbol
import com.gybra.terminallauncher.shell.ShellProfiles
import com.gybra.terminallauncher.shell.ShellType
import com.gybra.terminallauncher.theme.BadgeColor
import com.gybra.terminallauncher.theme.BadgeSize
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
    private val deviceLock: DeviceLock,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(LauncherPreferences().toUiState())
    private val writeMutex = Mutex()
    private var pendingWrites = 0
    private var batchFailed = false
    private var preferencesLoaded = false
    private var disclosureOffered = false
    public val uiState: StateFlow<SettingsUiState> = mutableUiState.asStateFlow()

    init {
        observePreferences()
    }

    public fun selectShell(shellType: ShellType) {
        updateSetting(
            update = { state -> state.copy(shellProfile = ShellProfiles.forType(shellType)) },
            persist = { preferencesRepository.setShellType(shellType) },
        )
    }

    public fun selectTheme(terminalTheme: TerminalTheme) {
        updateSetting(
            update = { state -> state.copy(terminalTheme = terminalTheme) },
            persist = { preferencesRepository.setTerminalTheme(terminalTheme) },
        )
    }

    public fun setBadgeBackground(color: String?) {
        if (color != null && !BadgeColor.isValid(color)) return
        updateSetting(
            update = { state -> state.copy(badgeBackground = color) },
            persist = { preferencesRepository.setBadgeBackground(color) },
        )
    }

    public fun setBadgeText(color: String?) {
        if (color != null && !BadgeColor.isValid(color)) return
        updateSetting(
            update = { state -> state.copy(badgeText = color) },
            persist = { preferencesRepository.setBadgeText(color) },
        )
    }

    public fun setBadgeSize(size: BadgeSize) {
        updateSetting(
            update = { state -> state.copy(badgeSize = size) },
            persist = { preferencesRepository.setBadgeSize(size) },
        )
    }

    public fun refreshNotificationAccess(enabled: Boolean) {
        mutableUiState.value = mutableUiState.value.copy(notificationAccess = enabled)
    }

    public fun setShowClock(showClock: Boolean) {
        updateSetting(
            update = { state -> state.copy(showClock = showClock) },
            persist = { preferencesRepository.setShowClock(showClock) },
        )
    }

    public fun setShowBattery(showBattery: Boolean) {
        updateSetting(
            update = { state -> state.copy(showBattery = showBattery) },
            persist = { preferencesRepository.setShowBattery(showBattery) },
        )
    }

    public fun setImmersiveMode(immersiveMode: Boolean) {
        updateSetting(
            update = { state -> state.copy(immersiveMode = immersiveMode) },
            persist = { preferencesRepository.setImmersiveMode(immersiveMode) },
        )
    }

    /** Requests separate consent before Android permission; turning the service off needs neither. */
    public fun setDoubleTapToLock(enabled: Boolean) {
        if (enabled) {
            performAccessibilityAction { deviceLock.requestEnable() }
        } else {
            deviceLock.disable()
        }
        refreshDeviceLock()
    }

    /** Gates all four global actions, including when a legacy service is already enabled. */
    public fun performAccessibilityAction(action: () -> Unit) {
        val state = mutableUiState.value
        if (!preferencesLoaded || state.savingAccessibilityConsent) return
        if (state.accessibilityConsent == AccessibilityConsent.GRANTED) {
            action()
        } else {
            mutableUiState.value = state.copy(accessibilityDisclosureVisible = true)
        }
    }

    public fun acceptAccessibilityDisclosure() {
        if (!canAnswerDisclosure()) return
        saveAccessibilityConsent(AccessibilityConsent.GRANTED)
    }

    public fun dismissAccessibilityDisclosure() {
        if (!canAnswerDisclosure()) return
        mutableUiState.value = mutableUiState.value.copy(accessibilityDisclosureVisible = false)
        saveAccessibilityConsent(AccessibilityConsent.DECLINED)
    }

    private fun canAnswerDisclosure(): Boolean = preferencesLoaded &&
        mutableUiState.value.accessibilityDisclosureVisible && !mutableUiState.value.savingAccessibilityConsent

    private fun saveAccessibilityConsent(consent: AccessibilityConsent) {
        mutableUiState.value = mutableUiState.value.copy(savingAccessibilityConsent = true, storageError = null)
        pendingWrites += 1
        viewModelScope.launch {
            try {
                writeMutex.withLock { preferencesRepository.setAccessibilityConsent(consent) }
                mutableUiState.value = mutableUiState.value.copy(
                    accessibilityConsent = consent,
                    accessibilityDisclosureVisible = false,
                )
                if (consent == AccessibilityConsent.GRANTED) deviceLock.requestEnable()
                refreshDeviceLock()
            } catch (_: IOException) {
                batchFailed = true
            }
            pendingWrites -= 1
            mutableUiState.value = mutableUiState.value.copy(savingAccessibilityConsent = false)
            if (pendingWrites == 0) refreshPersistedState()
        }
    }

    /** Reads the service again, keeping Android permission distinct from in-app consent. */
    public fun refreshDeviceLock() {
        val state = mutableUiState.value
        mutableUiState.value = state.copy(
            doubleTapToLock = deviceLock.enabled && state.accessibilityConsent == AccessibilityConsent.GRANTED,
        )
    }

    /** Stores [username] as a prompt token, so what is typed can always be written in a prompt. */
    public fun setUsername(username: String) {
        val token = username.asPromptToken()
        updateSetting(
            update = { state -> state.copy(username = token) },
            persist = { preferencesRepository.setUsername(token) },
        )
    }

    /** Stores [hostname] as a prompt token, under the same rule as the username. */
    public fun setHostname(hostname: String) {
        val token = hostname.asPromptToken()
        updateSetting(
            update = { state -> state.copy(hostname = token) },
            persist = { preferencesRepository.setHostname(token) },
        )
    }

    public fun selectPromptSymbol(promptSymbol: PromptSymbol) {
        updateSetting(
            update = { state -> state.copy(promptSymbol = promptSymbol) },
            persist = { preferencesRepository.setPromptSymbol(promptSymbol) },
        )
    }

    public fun setShowPromptPath(showPromptPath: Boolean) {
        updateSetting(
            update = { state -> state.copy(showPromptPath = showPromptPath) },
            persist = { preferencesRepository.setShowPromptPath(showPromptPath) },
        )
    }

    public fun selectDosDrive(dosDrive: DosDrive) {
        updateSetting(
            update = { state -> state.copy(dosDrive = dosDrive) },
            persist = { preferencesRepository.setDosDrive(dosDrive) },
        )
    }

    /**
     * Keeps an identity usable in a prompt: whitespace and control characters never reach it, and
     * it stays short enough to leave the rest of the line readable.
     */
    private fun String.asPromptToken(): String =
        filterNot { character -> character.isWhitespace() || character.isISOControl() }
            .take(MAX_PROMPT_TOKEN_LENGTH)

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesRepository.preferences.collect { preferences ->
                if (pendingWrites == 0) {
                    applyPreferences(preferences)
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
            val error = if (batchFailed) SettingsEntry.STORAGE_ERROR.label else null
            batchFailed = false
            applyPreferences(preferences, storageError = error)
        }
    }

    private fun applyPreferences(preferences: LauncherPreferences, storageError: String? = null) {
        val current = mutableUiState.value
        val offerDisclosure = !disclosureOffered && preferences.accessibilityConsent == AccessibilityConsent.UNDECIDED
        preferencesLoaded = true
        disclosureOffered = true
        mutableUiState.value = preferences.toUiState().copy(
            storageError = storageError,
            notificationAccess = current.notificationAccess,
            accessibilityDisclosureVisible = current.accessibilityDisclosureVisible || offerDisclosure,
            savingAccessibilityConsent = current.savingAccessibilityConsent,
        )
    }

    private fun LauncherPreferences.toUiState(): SettingsUiState = SettingsUiState(
        shellProfile = ShellProfiles.forType(shellType),
        terminalTheme = terminalTheme,
        badgeBackground = badgeBackground,
        badgeText = badgeText,
        badgeSize = badgeSize,
        showClock = showClock,
        showBattery = showBattery,
        immersiveMode = immersiveMode,
        doubleTapToLock = deviceLock.enabled && accessibilityConsent == AccessibilityConsent.GRANTED,
        accessibilityConsent = accessibilityConsent,
        username = username,
        hostname = hostname,
        promptSymbol = promptSymbol,
        showPromptPath = showPromptPath,
        dosDrive = dosDrive,
    )

    private companion object {
        const val MAX_PROMPT_TOKEN_LENGTH = 16
    }
}
