package com.gybra.terminallauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gybra.terminallauncher.shell.DosDrive
import com.gybra.terminallauncher.shell.PromptOption
import com.gybra.terminallauncher.shell.PromptSymbol
import com.gybra.terminallauncher.shell.ShellType
import com.gybra.terminallauncher.theme.BadgeSize
import com.gybra.terminallauncher.theme.TerminalTheme
import com.gybra.terminallauncher.ui.TestTag
import com.gybra.terminallauncher.ui.terminalTextStyle
import com.gybra.terminallauncher.ui.theme.LocalTerminalColors

/**
 * The settings, read from the top: what Home would look like, then the sections, collapsed until
 * one is opened. Opening a section closes the one already open, so the preview stays next to
 * whatever is being changed.
 */
@Composable
public fun SettingsScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTerminalColors.current
    var openSection by rememberSaveable { mutableStateOf<SettingsSection?>(null) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ActionLine(text = state.write(SettingsEntry.BACK.label), onClick = onBack)
        state.storageError?.let { error -> TerminalText(state.write(error)) }
        SettingsPreview(state = state)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .testTag(TestTag.SETTINGS_LIST.tag),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val toggle = { section: SettingsSection -> openSection = section.takeUnless { it == openSection } }
            section(state, SettingsSection.APPEARANCE, openSection, toggle) { AppearanceRows(state, actions) }
            section(state, SettingsSection.HOME_SCREEN, openSection, toggle) { HomeScreenRows(state, actions) }
            section(state, SettingsSection.GESTURES, openSection, toggle) {
                ToggleRow(state, SettingsEntry.DOUBLE_TAP_TO_LOCK, state.doubleTapToLock, actions.setDoubleTapToLock)
            }
            section(state, SettingsSection.PROMPT, openSection, toggle) { PromptRows(state, actions) }
            section(state, SettingsSection.NOTIFICATIONS, openSection, toggle) { NotificationRows(state, actions) }
        }
    }
}

private fun LazyListScope.section(
    state: SettingsUiState,
    section: SettingsSection,
    openSection: SettingsSection?,
    onToggle: (SettingsSection) -> Unit,
    rows: @Composable () -> Unit,
) {
    item(key = section.entry.key) {
        SectionBlock(
            state = state,
            section = section,
            open = openSection == section,
            onToggle = { onToggle(section) },
            rows = rows,
        )
    }
}

/** One section: a header carrying its summary, and its rows inside the same border once open. */
@Composable
private fun SectionBlock(
    state: SettingsUiState,
    section: SettingsSection,
    open: Boolean,
    onToggle: () -> Unit,
    rows: @Composable () -> Unit,
) {
    val colors = LocalTerminalColors.current
    val ink = if (open) colors.background else colors.foreground
    val openState = state.write(if (open) SettingsEntry.EXPANDED.label else SettingsEntry.COLLAPSED.label)
    Column(modifier = Modifier.border(1.dp, if (open) colors.foreground else colors.secondary)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .background(if (open) colors.foreground else Color.Transparent)
                .clickable(role = Role.Button, onClick = onToggle)
                .semantics { stateDescription = openState }
                .testTag(section.entry.key)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BasicText(
                text = "${if (open) OPEN_MARK else CLOSED_MARK} ${state.write(section.entry.label)}",
                style = terminalTextStyle(ink).copy(fontWeight = FontWeight.Bold),
            )
            BasicText(
                text = section.summarize(state),
                style = descriptionStyle(if (open) colors.background else colors.secondary)
                    .copy(textAlign = TextAlign.End),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (open) {
            Column(modifier = Modifier.padding(horizontal = 12.dp)) { rows() }
        }
    }
}

@Composable
private fun AppearanceRows(state: SettingsUiState, actions: SettingsActions) {
    ChoiceRow(
        state = state,
        entry = SettingsEntry.SHELL,
        options = ShellType.entries,
        selected = state.shellProfile.type,
        optionLabel = ShellType::name,
        onSelect = actions.selectShell,
    )
    ChoiceRow(
        state = state,
        entry = SettingsEntry.THEME,
        options = TerminalTheme.entries,
        selected = state.terminalTheme,
        optionLabel = TerminalTheme::name,
        onSelect = actions.selectTheme,
        leading = { theme -> ThemeSwatch(theme) },
    )
}

@Composable
private fun HomeScreenRows(state: SettingsUiState, actions: SettingsActions) {
    ToggleRow(state, SettingsEntry.SHOW_CLOCK, state.showClock, actions.setShowClock)
    ToggleRow(state, SettingsEntry.SHOW_BATTERY, state.showBattery, actions.setShowBattery)
    ToggleRow(state, SettingsEntry.IMMERSIVE_MODE, state.immersiveMode, actions.setImmersiveMode)
}

/** Writes the rows for the prompt parts the selected shell honours, which is its decision. */
@Composable
private fun PromptRows(state: SettingsUiState, actions: SettingsActions) {
    val options = state.shellProfile.promptOptions
    if (PromptOption.USERNAME in options) {
        TextRow(state, SettingsEntry.USERNAME, state.username, actions.setUsername)
    }
    if (PromptOption.HOSTNAME in options) {
        TextRow(state, SettingsEntry.HOSTNAME, state.hostname, actions.setHostname)
    }
    if (PromptOption.SYMBOL in options) {
        ChoiceRow(
            state = state,
            entry = SettingsEntry.PROMPT_SYMBOL,
            options = PromptSymbol.entries,
            selected = state.promptSymbol,
            optionLabel = PromptSymbol::text,
            onSelect = actions.selectPromptSymbol,
        )
    }
    if (PromptOption.DRIVE in options) {
        ChoiceRow(
            state = state,
            entry = SettingsEntry.DRIVE,
            options = DosDrive.entries,
            selected = state.dosDrive,
            optionLabel = { drive -> "${drive.name}:" },
            onSelect = actions.selectDosDrive,
        )
    }
    if (PromptOption.PATH in options) {
        ToggleRow(state, SettingsEntry.SHOW_PROMPT_PATH, state.showPromptPath, actions.setShowPromptPath)
    }
}

/** The badge settings change nothing until Android grants access, so they wait for it. */
@Composable
private fun NotificationRows(state: SettingsUiState, actions: SettingsActions) {
    NotificationAccessRow(state = state, onOpenSettings = actions.openNotificationAccess)
    if (!state.notificationAccess) return
    ChoiceRow(
        state = state,
        entry = SettingsEntry.BADGE_SIZE,
        options = BadgeSize.entries,
        selected = state.badgeSize,
        optionLabel = { size -> size.step.toString() },
        onSelect = actions.setBadgeSize,
    )
    BadgeColorRow(state, SettingsEntry.BADGE_BACKGROUND, state.badgeBackground, actions.setBadgeBackground)
    BadgeColorRow(state, SettingsEntry.BADGE_TEXT, state.badgeText, actions.setBadgeText)
}
