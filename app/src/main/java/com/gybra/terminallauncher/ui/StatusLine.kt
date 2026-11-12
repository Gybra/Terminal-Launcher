package com.gybra.terminallauncher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gybra.terminallauncher.ui.theme.LocalTerminalColors

/** Places the status parts on the two sides of the line, the way a status bar reads. */
@Composable
internal fun StatusLine(clock: String?, battery: String?, modifier: Modifier = Modifier) {
    val colors = LocalTerminalColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BasicText(text = clock.orEmpty(), style = terminalTextStyle(colors.secondary))
        BasicText(text = battery.orEmpty(), style = terminalTextStyle(colors.secondary))
    }
}
