package com.gybra.terminallauncher.ui.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gybra.terminallauncher.ui.settings.ActionLine
import com.gybra.terminallauncher.ui.terminalTextStyle
import com.gybra.terminallauncher.ui.theme.LocalTerminalColors

/** Dismissal is refusal, never acceptance; long text and both choices remain scrollable. */
@Composable
public fun AccessibilityDisclosure(
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    saving: Boolean,
    error: String?,
) {
    val colors = LocalTerminalColors.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = !saving, dismissOnClickOutside = !saving),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.background)
                .border(1.dp, colors.foreground)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BasicText(AccessibilityDisclosureText.TITLE, style = terminalTextStyle(colors.foreground))
            BasicText(AccessibilityDisclosureText.BODY, style = terminalTextStyle(colors.foreground))
            error?.let { BasicText(it, style = terminalTextStyle(colors.foreground)) }
            ActionLine(text = AccessibilityDisclosureText.AGREE, enabled = !saving, onClick = onAccept)
            ActionLine(text = AccessibilityDisclosureText.NOT_NOW, enabled = !saving, onClick = onDismiss)
        }
    }
}
