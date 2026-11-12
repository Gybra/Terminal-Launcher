package com.gybra.terminallauncher.ui.settings

/** The groups the settings screen opens one at a time, in the order it lists them. */
public enum class SettingsSection(public val entry: SettingsEntry) {
    APPEARANCE(SettingsEntry.APPEARANCE),
    HOME_SCREEN(SettingsEntry.HOME_SCREEN),
    GESTURES(SettingsEntry.GESTURES),
    PROMPT(SettingsEntry.PROMPT),
    NOTIFICATIONS(SettingsEntry.NOTIFICATIONS),
    ;

    /**
     * Writes what this section is set to, for its collapsed header. The prompt is written as the
     * shell writes it, so the identity keeps the case it was typed in.
     */
    public fun summarize(state: SettingsUiState): String = when (this) {
        APPEARANCE -> state.write(
            listOf(state.shellProfile.type.name, state.terminalTheme.name).joinSummary(),
        )
        HOME_SCREEN -> state.write(homeScreenSummary(state))
        GESTURES -> state.write(gesturesSummary(state))
        PROMPT -> state.shellProfile.prompt(state.shellContext())
        NOTIFICATIONS -> state.write(notificationsSummary(state))
    }
}

private fun homeScreenSummary(state: SettingsUiState): String = listOfNotNull(
    SettingsEntry.SHOW_CLOCK.takeIf { state.showClock },
    SettingsEntry.SHOW_BATTERY.takeIf { state.showBattery },
    SettingsEntry.IMMERSIVE_MODE.takeIf { state.immersiveMode },
).map(SettingsEntry::summary).ifEmpty { listOf(SettingsEntry.ALL_OFF.label) }.joinSummary()

private fun gesturesSummary(state: SettingsUiState): String = if (state.doubleTapToLock) {
    SettingsEntry.DOUBLE_TAP_TO_LOCK.summary
} else {
    SettingsEntry.OFF.label
}

private fun notificationsSummary(state: SettingsUiState): String = if (state.notificationAccess) {
    listOf(
        SettingsEntry.NOTIFICATION_ENABLED.label,
        "${SettingsEntry.BADGE_SIZE.summary} ${state.badgeSize.step}",
    ).joinSummary()
} else {
    SettingsEntry.OFF.label
}

private fun List<String>.joinSummary(): String = joinToString(separator = " · ")
