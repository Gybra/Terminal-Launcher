package com.gybra.terminallauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gybra.terminallauncher.theme.BadgeColor
import com.gybra.terminallauncher.theme.DosColor
import com.gybra.terminallauncher.theme.TerminalTheme
import com.gybra.terminallauncher.theme.colors
import com.gybra.terminallauncher.ui.terminalTextStyle
import com.gybra.terminallauncher.ui.theme.LocalTerminalColors

/**
 * One setting: its label in bold with what it changes written under it, an optional value on the
 * right, and the controls that change it below. Label, description and value are read as one node,
 * while each control below stays its own.
 */
@Composable
internal fun SettingRow(
    state: SettingsUiState,
    entry: SettingsEntry,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
    content: @Composable () -> Unit = {},
) {
    val colors = LocalTerminalColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = state.write(entry.label),
                    style = terminalTextStyle(colors.foreground).copy(fontWeight = FontWeight.Bold),
                )
                BasicText(text = state.write(entry.description), style = descriptionStyle(colors.secondary))
            }
            trailing()
        }
        content()
    }
}

/** A setting with one answer out of a few, laid out side by side and wrapping when they do not fit. */
@Composable
internal fun <T : Enum<T>> ChoiceRow(
    state: SettingsUiState,
    entry: SettingsEntry,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    leading: @Composable (T) -> Unit = {},
) {
    SettingRow(state = state, entry = entry) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                Choice(
                    text = state.write(optionLabel(option)),
                    selected = option == selected,
                    tag = entry.optionKey(option),
                    onClick = { onSelect(option) },
                ) {
                    leading(option)
                }
            }
        }
    }
}

/** One answer, inverted while it is the chosen one, the way a TTY marks a selection. */
@Composable
private fun Choice(
    text: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
) {
    val colors = LocalTerminalColors.current
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .border(1.dp, if (selected) colors.foreground else colors.secondary)
            .background(if (selected) colors.foreground else Color.Transparent)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .testTag(tag)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        leading()
        BasicText(
            text = text,
            style = terminalTextStyle(if (selected) colors.background else colors.foreground)
                .copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
        )
    }
}

/** A small square of [theme]'s background holding a dot of its foreground. */
@Composable
internal fun ThemeSwatch(theme: TerminalTheme) {
    val swatch = theme.colors(isSystemInDarkTheme())
    Box(
        modifier = Modifier
            .size(16.dp)
            .background(swatch.background)
            .border(1.dp, swatch.foreground),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.size(6.dp).background(swatch.foreground))
    }
}

/** A setting that is on or off; the whole row toggles it. */
@Composable
internal fun ToggleRow(
    state: SettingsUiState,
    entry: SettingsEntry,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = LocalTerminalColors.current
    SettingRow(
        state = state,
        entry = entry,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange),
        trailing = {
            BasicText(
                text = state.write(if (checked) CHECKED_BOX else EMPTY_BOX),
                style = terminalTextStyle(if (checked) colors.background else colors.foreground),
                modifier = Modifier.background(if (checked) colors.foreground else Color.Transparent),
            )
        },
    )
}

/** A setting typed after a `>`, on a line underlined the way a terminal field reads. */
@Composable
internal fun TextRow(
    state: SettingsUiState,
    entry: SettingsEntry,
    value: String,
    onValueChange: (String) -> Unit,
) {
    val colors = LocalTerminalColors.current
    val label = state.write(entry.label)
    SettingRow(state = state, entry = entry) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BasicText(text = INPUT_MARK, style = terminalTextStyle(colors.secondary))
            Column(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = terminalTextStyle(colors.foreground),
                    cursorBrush = SolidColor(colors.foreground),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .semantics { contentDescription = label },
                )
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.secondary))
            }
        }
    }
}

/**
 * A badge colour: the current one is written on the right, and tapping the row opens the palette
 * under it until it is tapped again.
 */
@Composable
internal fun BadgeColorRow(
    state: SettingsUiState,
    entry: SettingsEntry,
    color: String?,
    onChange: (String?) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Column {
        SettingRow(
            state = state,
            entry = entry,
            modifier = Modifier.clickable(role = Role.Button) { open = !open },
            trailing = { BadgeColorValue(state = state, color = color, open = open) },
        )
        if (open) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DosColor.entries.chunked(4).forEach { row ->
                    ColorRow(entry = entry, row = row, selected = color, onSelect = onChange)
                }
                ActionLine(text = state.write(SettingsEntry.BADGE_RESET.label), onClick = { onChange(null) })
            }
        }
    }
}

@Composable
private fun BadgeColorValue(state: SettingsUiState, color: String?, open: Boolean) {
    val colors = LocalTerminalColors.current
    val name = DosColor.entries.firstOrNull { dosColor -> dosColor.hex.equals(color, ignoreCase = true) }
        ?.name?.replace('_', ' ')
        ?: SettingsEntry.BADGE_DEFAULT.label
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(BadgeColor.parse(color) ?: Color.Transparent)
                .border(1.dp, colors.secondary),
        )
        BasicText(
            text = "${state.write(name)} ${if (open) OPEN_MARK else CLOSED_MARK}",
            style = terminalTextStyle(colors.foreground),
        )
    }
}

@Composable
private fun ColorRow(
    entry: SettingsEntry,
    row: List<DosColor>,
    selected: String?,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        row.forEach { dosColor ->
            ColorSwatch(
                tag = entry.optionKey(dosColor),
                dosColor = dosColor,
                selected = selected.equals(dosColor.hex, ignoreCase = true),
                onSelect = { onSelect(dosColor.hex) },
            )
        }
    }
}

@Composable
private fun RowScope.ColorSwatch(
    tag: String,
    dosColor: DosColor,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val colors = LocalTerminalColors.current
    Box(
        modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onSelect,
            )
            .border(2.dp, if (selected) colors.foreground else colors.secondary)
            .background(dosColor.color)
            .semantics { contentDescription = dosColor.name }
            .testTag(tag),
    )
}

/** Whether Android lets the launcher read notifications, with the way to grant it while it does not. */
@Composable
internal fun NotificationAccessRow(state: SettingsUiState, onOpenSettings: () -> Unit) {
    val colors = LocalTerminalColors.current
    val status = if (state.notificationAccess) SettingsEntry.NOTIFICATION_ENABLED else SettingsEntry.OFF
    SettingRow(
        state = state,
        entry = SettingsEntry.NOTIFICATION_ACCESS,
        trailing = { BasicText(text = state.write(status.label), style = terminalTextStyle(colors.foreground)) },
    ) {
        if (!state.notificationAccess) {
            ActionLine(text = state.write(SettingsEntry.NOTIFICATION_SETTINGS.label), onClick = onOpenSettings)
        }
    }
}

@Composable
internal fun ActionLine(text: String, onClick: () -> Unit) {
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
internal fun TerminalText(text: String) {
    val colors = LocalTerminalColors.current
    BasicText(text = text, style = terminalTextStyle(colors.foreground))
}

/** The smaller arrested text that explains or summarises a setting. */
internal fun descriptionStyle(color: Color): TextStyle =
    terminalTextStyle(color = color, fontSize = 14.sp, lineHeight = 20.sp)

private const val CHECKED_BOX = "[x]"
private const val EMPTY_BOX = "[ ]"
private const val INPUT_MARK = ">"
/** Marks something that opens in place, once open and while closed. */
internal const val OPEN_MARK: String = "▾"
internal const val CLOSED_MARK: String = "▸"
