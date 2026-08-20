package com.gybra.terminallauncher.ui.pin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gybra.terminallauncher.ui.terminalTextStyle
import com.gybra.terminallauncher.ui.theme.LocalTerminalColors

/** Asks whether the shortcut an application offers may stay on Home. */
@Composable
public fun PinShortcutScreen(
    state: PinShortcutUiState,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTerminalColors.current
    val shell = state.shellProfile
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TerminalLine(text = shell.formatMessage("${state.shortcut.packageName} asks to pin"))
        TerminalLine(text = shell.formatShortcutName(state.shortcut))
        AnswerLine(text = shell.formatMessage("[ pin ]"), onClick = onAccept)
        AnswerLine(text = shell.formatMessage("[ cancel ]"), onClick = onDecline)
    }
}

@Composable
private fun AnswerLine(text: String, onClick: () -> Unit) {
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
private fun TerminalLine(text: String) {
    val colors = LocalTerminalColors.current
    BasicText(text = text, style = terminalTextStyle(colors.foreground))
}
