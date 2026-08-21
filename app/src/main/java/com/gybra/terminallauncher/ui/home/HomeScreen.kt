package com.gybra.terminallauncher.ui.home

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
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gybra.terminallauncher.command.Command
import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.launcher.AppShortcut
import com.gybra.terminallauncher.shell.ShellContext
import com.gybra.terminallauncher.shell.ShellProfile
import com.gybra.terminallauncher.ui.TestTag
import com.gybra.terminallauncher.ui.terminalTextStyle
import com.gybra.terminallauncher.ui.theme.LocalTerminalColors

@Composable
public fun HomeScreen(
    state: HomeUiState,
    onAppClick: (InstalledApp) -> Unit,
    onShortcutClick: (AppShortcut) -> Unit,
    onSettingsClick: () -> Unit,
    onLockScreen: () -> Unit,
    promptActions: PromptActions,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTerminalColors.current
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .pointerInput(onLockScreen) {
                detectTapGestures(onDoubleTap = { onLockScreen() })
            }
            .testTag(TestTag.HOME_LIST.tag),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (state.statusClock != null || state.statusBattery != null) {
            item(key = HomeItem.STATUS.key) {
                StatusLine(clock = state.statusClock, battery = state.statusBattery)
            }
        }
        pinnedItems(state = state, onAppClick = onAppClick, onShortcutClick = onShortcutClick)
        item(key = HomeItem.SETTINGS.key) {
            AppRow(
                displayName = state.shellProfile.aliasFor(Command.SETTINGS),
                onClick = onSettingsClick,
            )
        }
        terminalHistory(
            entries = state.history,
            shellProfile = state.shellProfile,
            shellContext = state.shellContext,
            onShortcutClick = onShortcutClick,
        )
        item(key = HomeItem.PROMPT.key) {
            Prompt(
                prompt = state.shellProfile.prompt(state.shellContext),
                state = state.prompt,
                actions = promptActions,
            )
        }
        items(
            items = state.searchResults,
            key = { result -> HomeItem.SEARCH.rowKey(result.app.packageName) },
        ) { result ->
            AppRow(
                displayName = state.shellProfile.formatAppName(result.app),
                onClick = { onAppClick(result.app) },
            )
        }
    }
}

/** Lists what Home keeps above the prompt: the pinned applications, then the pinned shortcuts. */
private fun LazyListScope.pinnedItems(
    state: HomeUiState,
    onAppClick: (InstalledApp) -> Unit,
    onShortcutClick: (AppShortcut) -> Unit,
) {
    items(
        items = state.apps,
        key = InstalledApp::packageName,
    ) { app ->
        AppRow(
            displayName = state.shellProfile.formatAppName(app),
            onClick = { onAppClick(app) },
        )
    }
    items(
        items = state.shortcuts,
        key = { shortcut -> HomeItem.SHORTCUT.rowKey("${shortcut.packageName}-${shortcut.id}") },
    ) { shortcut ->
        AppRow(
            displayName = state.shellProfile.formatShortcutName(shortcut),
            onClick = { onShortcutClick(shortcut) },
        )
    }
}

/** Writes what every submitted line printed, keeping the shortcuts it listed startable. */
private fun LazyListScope.terminalHistory(
    entries: List<TerminalEntry>,
    shellProfile: ShellProfile,
    shellContext: ShellContext,
    onShortcutClick: (AppShortcut) -> Unit,
) {
    items(
        items = entries,
        key = { entry -> HomeItem.HISTORY.rowKey(entry.id.toString()) },
    ) { entry ->
        Column {
            TerminalLine(text = "${shellProfile.prompt(shellContext)} ${entry.input}")
            entry.output.forEach { line -> TerminalLine(text = line) }
            entry.shortcuts.forEach { shortcut ->
                AppRow(
                    displayName = shellProfile.formatShortcutName(shortcut),
                    onClick = { onShortcutClick(shortcut) },
                )
            }
        }
    }
}

/** Places the status parts on the two sides of the line, the way a status bar reads. */
@Composable
private fun StatusLine(clock: String?, battery: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TerminalLine(text = clock.orEmpty())
        TerminalLine(text = battery.orEmpty())
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
