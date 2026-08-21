package com.gybra.terminallauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gybra.terminallauncher.shell.DosDrive
import com.gybra.terminallauncher.shell.PromptSymbol
import com.gybra.terminallauncher.shell.ShellType
import com.gybra.terminallauncher.theme.TerminalTheme
import com.gybra.terminallauncher.ui.TestTag
import com.gybra.terminallauncher.ui.terminalTextStyle
import com.gybra.terminallauncher.ui.theme.LocalTerminalColors

@Composable
public fun SettingsScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTerminalColors.current
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .testTag(TestTag.SETTINGS_LIST.tag),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = SettingsEntry.BACK.key) {
            ActionLine(text = SettingsEntry.BACK.label, onClick = onBack)
        }
        state.storageError?.let { error ->
            item(key = SettingsEntry.STORAGE_ERROR.key) { TerminalText(error) }
        }
        appearanceSettings(state = state, actions = actions)
        homeSettings(state = state, actions = actions)
        unixSettings(state = state, actions = actions)
        dosSettings(state = state, actions = actions)
    }
}

private fun LazyListScope.appearanceSettings(
    state: SettingsUiState,
    actions: SettingsActions,
) {
    item(key = SettingsEntry.APPEARANCE.key) { SectionTitle(SettingsEntry.APPEARANCE.label) }
    item(key = SettingsEntry.SHELL.key) { TerminalText(SettingsEntry.SHELL.label) }
    ShellType.entries.forEach { shellType ->
        item(key = SettingsEntry.SHELL.optionKey(shellType)) {
            SelectionOption(
                label = shellType.name,
                selected = state.shellType == shellType,
                onClick = { actions.selectShell(shellType) },
            )
        }
    }
    item(key = SettingsEntry.THEME.key) { TerminalText(SettingsEntry.THEME.label) }
    TerminalTheme.entries.forEach { terminalTheme ->
        item(key = SettingsEntry.THEME.optionKey(terminalTheme)) {
            SelectionOption(
                label = terminalTheme.name,
                selected = state.terminalTheme == terminalTheme,
                onClick = { actions.selectTheme(terminalTheme) },
            )
        }
    }
}

/** Lists what Home shows and how it behaves, under the same section as the appearance. */
private fun LazyListScope.homeSettings(
    state: SettingsUiState,
    actions: SettingsActions,
) {
    item(key = SettingsEntry.SHOW_CLOCK.key) {
        ToggleOption(
            label = SettingsEntry.SHOW_CLOCK.label,
            checked = state.showClock,
            onCheckedChange = actions.setShowClock,
        )
    }
    item(key = SettingsEntry.SHOW_BATTERY.key) {
        ToggleOption(
            label = SettingsEntry.SHOW_BATTERY.label,
            checked = state.showBattery,
            onCheckedChange = actions.setShowBattery,
        )
    }
    item(key = SettingsEntry.IMMERSIVE_MODE.key) {
        ToggleOption(
            label = SettingsEntry.IMMERSIVE_MODE.label,
            checked = state.immersiveMode,
            onCheckedChange = actions.setImmersiveMode,
        )
    }
    item(key = SettingsEntry.DOUBLE_TAP_TO_LOCK.key) {
        ToggleOption(
            label = SettingsEntry.DOUBLE_TAP_TO_LOCK.label,
            checked = state.doubleTapToLock,
            onCheckedChange = actions.setDoubleTapToLock,
        )
    }
    item(key = SettingsEntry.SHOW_PROMPT_PATH.key) {
        ToggleOption(
            label = SettingsEntry.SHOW_PROMPT_PATH.label,
            checked = state.showPromptPath,
            onCheckedChange = actions.setShowPromptPath,
        )
    }
}

private fun LazyListScope.unixSettings(
    state: SettingsUiState,
    actions: SettingsActions,
) {
    item(key = SettingsEntry.UNIX.key) { SectionTitle(SettingsEntry.UNIX.label) }
    item(key = SettingsEntry.USERNAME.key) {
        TextSetting(
            label = SettingsEntry.USERNAME.label,
            value = state.username,
            onValueChange = actions.setUsername,
        )
    }
    item(key = SettingsEntry.HOSTNAME.key) {
        TextSetting(
            label = SettingsEntry.HOSTNAME.label,
            value = state.hostname,
            onValueChange = actions.setHostname,
        )
    }
    item(key = SettingsEntry.PROMPT_SYMBOL.key) { TerminalText(SettingsEntry.PROMPT_SYMBOL.label) }
    PromptSymbol.entries.forEach { promptSymbol ->
        item(key = SettingsEntry.PROMPT_SYMBOL.optionKey(promptSymbol)) {
            SelectionOption(
                label = promptSymbol.text,
                selected = state.promptSymbol == promptSymbol,
                onClick = { actions.selectPromptSymbol(promptSymbol) },
            )
        }
    }
}

private fun LazyListScope.dosSettings(
    state: SettingsUiState,
    actions: SettingsActions,
) {
    item(key = SettingsEntry.DOS.key) { SectionTitle(SettingsEntry.DOS.label) }
    item(key = SettingsEntry.DRIVE.key) { TerminalText(SettingsEntry.DRIVE.label) }
    DosDrive.entries.forEach { dosDrive ->
        item(key = SettingsEntry.DRIVE.optionKey(dosDrive)) {
            SelectionOption(
                label = "${dosDrive.name}:",
                selected = state.dosDrive == dosDrive,
                onClick = { actions.selectDosDrive(dosDrive) },
            )
        }
    }
}

@Composable
private fun SelectionOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TerminalText("${if (selected) "(*)" else "( )"} $label")
    }
}

@Composable
private fun ToggleOption(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TerminalText("[${if (checked) "*" else " "}] $label")
    }
}

@Composable
private fun TextSetting(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    val colors = LocalTerminalColors.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TerminalText(label)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = terminalTextStyle(colors.foreground),
            cursorBrush = SolidColor(colors.foreground),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.secondary.copy(alpha = 0.2f))
                .padding(12.dp)
                .semantics { contentDescription = label },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    val colors = LocalTerminalColors.current
    BasicText(
        text = text,
        style = terminalTextStyle(colors.secondary),
        modifier = Modifier.padding(top = 16.dp),
    )
}

@Composable
private fun ActionLine(text: String, onClick: () -> Unit) {
    val colors = LocalTerminalColors.current
    BasicText(
        text = text,
        style = terminalTextStyle(colors.foreground),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick),
    )
}

@Composable
private fun TerminalText(text: String) {
    val colors = LocalTerminalColors.current
    BasicText(text = text, style = terminalTextStyle(colors.foreground))
}
