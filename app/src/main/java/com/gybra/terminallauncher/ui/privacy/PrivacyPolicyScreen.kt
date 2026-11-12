package com.gybra.terminallauncher.ui.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gybra.terminallauncher.ui.settings.ActionLine
import com.gybra.terminallauncher.ui.settings.SettingsEntry
import com.gybra.terminallauncher.ui.terminalTextStyle
import com.gybra.terminallauncher.ui.theme.LocalTerminalColors

/** Keeps policy text in its original case in both shell profiles. */
@Composable
public fun PrivacyPolicyScreen(onBack: () -> Unit) {
    val colors = LocalTerminalColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ActionLine(text = SettingsEntry.BACK.label, onClick = onBack)
        BasicText(PrivacyPolicy.TITLE, style = terminalTextStyle(colors.foreground))
        BasicText(PrivacyPolicy.BODY, style = terminalTextStyle(colors.foreground))
        BasicText(PrivacyPolicy.CONTACT, style = terminalTextStyle(colors.foreground))
    }
}
