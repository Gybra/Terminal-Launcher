package com.gybra.terminallauncher.ui.settings

import com.gybra.terminallauncher.preferences.AccessibilityConsent
import com.gybra.terminallauncher.shell.DosDrive
import com.gybra.terminallauncher.shell.LauncherLocation
import com.gybra.terminallauncher.shell.PromptSymbol
import com.gybra.terminallauncher.shell.ShellContext
import com.gybra.terminallauncher.shell.ShellProfile
import com.gybra.terminallauncher.theme.BadgeSize
import com.gybra.terminallauncher.theme.TerminalTheme

public data class SettingsUiState(
    public val shellProfile: ShellProfile,
    public val terminalTheme: TerminalTheme,
    public val showClock: Boolean,
    public val showBattery: Boolean,
    public val immersiveMode: Boolean,
    public val doubleTapToLock: Boolean,
    public val username: String,
    public val hostname: String,
    public val promptSymbol: PromptSymbol,
    public val showPromptPath: Boolean,
    public val dosDrive: DosDrive,
    public val storageError: String? = null,
    public val notificationAccess: Boolean = false,
    public val badgeBackground: String? = null,
    public val badgeText: String? = null,
    public val badgeSize: BadgeSize = BadgeSize.TWO,
    public val accessibilityConsent: AccessibilityConsent = AccessibilityConsent.UNDECIDED,
    public val accessibilityDisclosureVisible: Boolean = false,
    public val savingAccessibilityConsent: Boolean = false,
) {
    /**
     * Writes [text] the way the selected shell writes a line, so the settings read in the same
     * voice as Home instead of keeping a face of their own.
     */
    public fun write(text: String): String = shellProfile.formatMessage(text)

    /** What Home would write its prompt from with these settings, standing at Home. */
    public fun shellContext(): ShellContext = ShellContext(
        username = username,
        hostname = hostname,
        location = LauncherLocation.HOME,
        promptSymbol = promptSymbol,
        showPath = showPromptPath,
        dosDrive = dosDrive,
    )
}
