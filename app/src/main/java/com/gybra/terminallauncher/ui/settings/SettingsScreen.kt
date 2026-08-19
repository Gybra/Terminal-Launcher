package com.gybra.terminallauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import com.gybra.terminallauncher.shell.ShellType
import com.gybra.terminallauncher.theme.TerminalTheme
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
            .testTag("settings-list"),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "back") { ActionLine(text = "< back", onClick = onBack) }
        state.storageError?.let { error ->
            item(key = "storage-error") { TerminalText(error) }
        }
        appearanceSettings(state = state, actions = actions)
        unixSettings(state = state, actions = actions)
    }
}

private fun LazyListScope.appearanceSettings(
    state: SettingsUiState,
    actions: SettingsActions,
) {
    item(key = "appearance") { SectionTitle("Appearance") }
    item(key = "shell") { TerminalText("Shell") }
    ShellType.entries.forEach { shellType ->
        item(key = "shell-${shellType.name}") {
            SelectionOption(
                label = shellType.name,
                selected = state.shellType == shellType,
                onClick = { actions.selectShell(shellType) },
            )
        }
    }
    item(key = "theme") { TerminalText("Theme") }
    TerminalTheme.entries.forEach { terminalTheme ->
        item(key = "theme-${terminalTheme.name}") {
            SelectionOption(
                label = terminalTheme.name,
                selected = state.terminalTheme == terminalTheme,
                onClick = { actions.selectTheme(terminalTheme) },
            )
        }
    }
    item(key = "clock") {
        ToggleOption(
            label = "Show clock",
            checked = state.showClock,
            onCheckedChange = actions.setShowClock,
        )
    }
}

private fun LazyListScope.unixSettings(
    state: SettingsUiState,
    actions: SettingsActions,
) {
    item(key = "unix") { SectionTitle("Unix") }
    item(key = "username") {
        TextSetting(
            label = "Username",
            value = state.username,
            onValueChange = actions.setUsername,
        )
    }
    item(key = "hostname") {
        TextSetting(
            label = "Hostname",
            value = state.hostname,
            onValueChange = actions.setHostname,
        )
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
