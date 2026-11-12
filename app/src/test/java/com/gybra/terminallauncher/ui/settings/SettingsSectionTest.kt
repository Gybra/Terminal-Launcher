package com.gybra.terminallauncher.ui.settings

import com.gybra.terminallauncher.shell.DosDrive
import com.gybra.terminallauncher.shell.PromptSymbol
import com.gybra.terminallauncher.shell.dos.DosShellProfile
import com.gybra.terminallauncher.shell.unix.UnixShellProfile
import com.gybra.terminallauncher.theme.BadgeSize
import com.gybra.terminallauncher.theme.TerminalTheme
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsSectionTest {
    @Test
    fun `appearance summarises the shell and the theme in the shell voice`() {
        assertEquals("unix · solarized", SettingsSection.APPEARANCE.summarize(state()))
        assertEquals(
            "DOS · SOLARIZED",
            SettingsSection.APPEARANCE.summarize(state().copy(shellProfile = DosShellProfile)),
        )
    }

    @Test
    fun `home screen lists what it shows or says that everything is off`() {
        assertEquals("clock · battery · immersive", SettingsSection.HOME_SCREEN.summarize(state()))
        assertEquals(
            "battery",
            SettingsSection.HOME_SCREEN.summarize(
                state().copy(showClock = false, immersiveMode = false),
            ),
        )
        assertEquals(
            "all off",
            SettingsSection.HOME_SCREEN.summarize(
                state().copy(showClock = false, showBattery = false, immersiveMode = false),
            ),
        )
    }

    @Test
    fun `gestures say whether a double tap locks`() {
        assertEquals("off", SettingsSection.GESTURES.summarize(state()))
        assertEquals(
            "double tap locks",
            SettingsSection.GESTURES.summarize(state().copy(doubleTapToLock = true)),
        )
    }

    @Test
    fun `prompt is summarised by the prompt itself, keeping the case of the identity`() {
        assertEquals("Oreste@Phone:~$", SettingsSection.PROMPT.summarize(state()))
        assertEquals(
            "~$",
            SettingsSection.PROMPT.summarize(state().copy(username = "", hostname = "")),
        )
        assertEquals(
            "D:\\HOME>",
            SettingsSection.PROMPT.summarize(
                state().copy(shellProfile = DosShellProfile, dosDrive = DosDrive.D),
            ),
        )
    }

    @Test
    fun `notifications report the access and the badge size`() {
        assertEquals("off", SettingsSection.NOTIFICATIONS.summarize(state()))
        assertEquals(
            "granted · size 2",
            SettingsSection.NOTIFICATIONS.summarize(state().copy(notificationAccess = true)),
        )
    }

    private fun state(): SettingsUiState = SettingsUiState(
        shellProfile = UnixShellProfile,
        terminalTheme = TerminalTheme.SOLARIZED,
        showClock = true,
        showBattery = true,
        immersiveMode = true,
        doubleTapToLock = false,
        username = "Oreste",
        hostname = "Phone",
        promptSymbol = PromptSymbol.DOLLAR,
        showPromptPath = true,
        dosDrive = DosDrive.C,
        badgeSize = BadgeSize.TWO,
    )
}
