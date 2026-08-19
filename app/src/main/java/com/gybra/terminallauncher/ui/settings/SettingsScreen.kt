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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gybra.terminallauncher.shell.ShellType
import com.gybra.terminallauncher.ui.terminalTextStyle

@Composable
public fun SettingsScreen(
    state: SettingsUiState,
    onShellSelected: (ShellType) -> Unit,
    onShowClockChanged: (Boolean) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onHostnameChanged: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("settings-list"),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "back") { ActionLine(text = "< back", onClick = onBack) }
        state.storageError?.let { error ->
            item(key = "storage-error") { TerminalText(error) }
        }
        item(key = "appearance") { SectionTitle("Appearance") }
        item(key = "shell") { TerminalText("Shell") }
        ShellType.entries.forEach { shellType ->
            item(key = shellType.name) {
                ShellOption(
                    shellType = shellType,
                    selected = state.shellType == shellType,
                    onSelected = onShellSelected,
                )
            }
        }
        item(key = "clock") {
            ToggleOption(
                label = "Show clock",
                checked = state.showClock,
                onCheckedChange = onShowClockChanged,
            )
        }
        item(key = "unix") { SectionTitle("Unix") }
        item(key = "username") {
            TextSetting(
                label = "Username",
                value = state.username,
                onValueChange = onUsernameChanged,
            )
        }
        item(key = "hostname") {
            TextSetting(
                label = "Hostname",
                value = state.hostname,
                onValueChange = onHostnameChanged,
            )
        }
    }
}

@Composable
private fun ShellOption(
    shellType: ShellType,
    selected: Boolean,
    onSelected: (ShellType) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = { onSelected(shellType) },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TerminalText("${if (selected) "(*)" else "( )"} ${shellType.name}")
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
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TerminalText(label)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = terminalTextStyle,
            cursorBrush = SolidColor(Color.White),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF181818))
                .padding(12.dp)
                .semantics { contentDescription = label },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    BasicText(
        text = text,
        style = terminalTextStyle.copy(color = Color(0xFFAAAAAA)),
        modifier = Modifier.padding(top = 16.dp),
    )
}

@Composable
private fun ActionLine(text: String, onClick: () -> Unit) {
    BasicText(
        text = text,
        style = terminalTextStyle,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick),
    )
}

@Composable
private fun TerminalText(text: String) {
    BasicText(text = text, style = terminalTextStyle)
}
