package com.gybra.terminallauncher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gybra.terminallauncher.command.Command
import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.ui.terminalTextStyle
import com.gybra.terminallauncher.ui.theme.LocalTerminalColors

@Composable
public fun HomeScreen(
    state: HomeUiState,
    onAppClick: (InstalledApp) -> Unit,
    onSettingsClick: () -> Unit,
    promptActions: PromptActions,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTerminalColors.current
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag("home-list"),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        state.clockText?.let { clockText ->
            item(key = "clock") { TerminalLine(text = clockText) }
        }
        items(
            items = state.apps,
            key = InstalledApp::packageName,
        ) { app ->
            AppRow(
                displayName = state.shellProfile.formatAppName(app),
                onClick = { onAppClick(app) },
            )
        }
        item(key = "settings") {
            AppRow(
                displayName = state.shellProfile.aliasFor(Command.SETTINGS),
                onClick = onSettingsClick,
            )
        }
        terminalHistory(
            entries = state.history,
            prompt = state.shellProfile.prompt(state.shellContext),
        )
        item(key = "prompt") {
            Prompt(
                prompt = state.shellProfile.prompt(state.shellContext),
                state = state.prompt,
                actions = promptActions,
            )
        }
        items(
            items = state.searchResults,
            key = { result -> "search-${result.app.packageName}" },
        ) { result ->
            AppRow(
                displayName = state.shellProfile.formatAppName(result.app),
                onClick = { onAppClick(result.app) },
            )
        }
    }
}

private fun LazyListScope.terminalHistory(entries: List<TerminalEntry>, prompt: String) {
    items(
        items = entries,
        key = { entry -> "history-${entry.id}" },
    ) { entry ->
        Column {
            TerminalLine(text = "$prompt ${entry.input}")
            entry.output.forEach { line -> TerminalLine(text = line) }
        }
    }
}

@Composable
private fun AppRow(
    displayName: String,
    onClick: () -> Unit,
) {
    val colors = LocalTerminalColors.current
    BasicText(
        text = displayName,
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
    BasicText(
        text = text,
        style = terminalTextStyle(colors.foreground),
    )
}
