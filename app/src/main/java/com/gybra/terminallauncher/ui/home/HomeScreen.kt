package com.gybra.terminallauncher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
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
    onLockScreen: () -> Unit,
    promptActions: PromptActions,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTerminalColors.current
    val promptFocus = remember { FocusRequester() }
    val rowActions = RowActions(
        onAppClick = onAppClick,
        onShortcutClick = onShortcutClick,
        promptActions = promptActions,
        promptFocus = promptFocus,
    )
    val rows = rememberLazyListState()
    val lastRow = state.rowCount - 1
    LaunchedEffect(lastRow, state.history.lastOrNull()?.id, state.searchResults) {
        if (lastRow >= 0) {
            rows.animateScrollToItem(lastRow)
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .pointerInput(onLockScreen) {
                detectTapGestures(onDoubleTap = { onLockScreen() })
            }
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
    ) {
        if (state.statusClock != null || state.statusBattery != null) {
            StatusLine(clock = state.statusClock, battery = state.statusBattery)
        }
        LazyColumn(
            state = rows,
            modifier = Modifier
                .weight(1f)
                .testTag(TestTag.HOME_LIST.tag),
            verticalArrangement = Arrangement.Bottom,
        ) {
            helpInvitation(line = state.helpInvitation)
            pinnedItems(state = state, rowActions = rowActions)
            terminalHistory(
                entries = state.history,
                shellProfile = state.shellProfile,
                shellContext = state.shellContext,
                rowActions = rowActions,
            )
            searchResults(state = state, rowActions = rowActions)
        }
        Prompt(
            prompt = state.shellProfile.prompt(state.shellContext),
            cursor = state.shellProfile.cursor,
            state = state.prompt,
            actions = promptActions,
            focusRequester = promptFocus,
        )
    }
}

/**
 * How many rows the scrolling region holds, so Home can reach its last one and keep what just
 * happened next to the prompt.
 */
private val HomeUiState.rowCount: Int
    get() = apps.size + shortcuts.size + history.size + searchResults.size +
        (if (helpInvitation == null) 0 else 1)

/** Writes the line an empty Home reads, which the shell wrote and nothing here can start. */
private fun LazyListScope.helpInvitation(line: String?) {
    if (line != null) {
        item(key = HomeItem.HELP_INVITATION.key) {
            TerminalLine(text = line)
        }
    }
}

/**
 * What a row does when it is tapped and when it is held, carried together so every list writes
 * rows that answer the same two gestures. A tap starts what the row names, while a long press
 * only writes the command it offers and moves to the prompt, where the user reads it and submits.
 */
private class RowActions(
    val onAppClick: (InstalledApp) -> Unit,
    val onShortcutClick: (AppShortcut) -> Unit,
    private val promptActions: PromptActions,
    private val promptFocus: FocusRequester,
) {
    fun onAppLongClick(app: InstalledApp) {
        promptActions.writeAppCommand(app)
        promptFocus.requestFocus()
    }

    fun onShortcutLongClick(shortcut: AppShortcut) {
        promptActions.writeShortcutCommand(shortcut)
        promptFocus.requestFocus()
    }
}

/** Lists what the typed line matches, right above the prompt that is matching it. */
private fun LazyListScope.searchResults(
    state: HomeUiState,
    rowActions: RowActions,
) {
    items(
        items = state.searchResults,
        key = { result -> HomeItem.SEARCH.rowKey(result.app.packageName) },
    ) { result ->
        AppRow(
            displayName = state.shellProfile.formatAppName(result.app),
            onClick = { rowActions.onAppClick(result.app) },
            onLongClick = { rowActions.onAppLongClick(result.app) },
        )
    }
}

/** Lists what Home keeps above the prompt: the pinned applications, then the pinned shortcuts. */
private fun LazyListScope.pinnedItems(
    state: HomeUiState,
    rowActions: RowActions,
) {
    items(
        items = state.apps,
        key = InstalledApp::packageName,
    ) { app ->
        AppRow(
            displayName = state.shellProfile.formatAppName(app),
            onClick = { rowActions.onAppClick(app) },
            onLongClick = { rowActions.onAppLongClick(app) },
        )
    }
    items(
        items = state.shortcuts,
        key = { shortcut -> HomeItem.SHORTCUT.rowKey("${shortcut.packageName}-${shortcut.id}") },
    ) { shortcut ->
        AppRow(
            displayName = state.shellProfile.formatShortcutName(shortcut),
            onClick = { rowActions.onShortcutClick(shortcut) },
            onLongClick = { rowActions.onShortcutLongClick(shortcut) },
        )
    }
}

/**
 * Writes what every submitted line printed, keeping the applications and shortcuts it listed
 * startable.
 */
private fun LazyListScope.terminalHistory(
    entries: List<TerminalEntry>,
    shellProfile: ShellProfile,
    shellContext: ShellContext,
    rowActions: RowActions,
) {
    items(
        items = entries,
        key = { entry -> HomeItem.HISTORY.rowKey(entry.id.toString()) },
    ) { entry ->
        Column {
            TerminalLine(text = "${shellProfile.prompt(shellContext)} ${entry.input}")
            entry.output.forEach { line -> TerminalLine(text = line) }
            entry.apps.forEach { app ->
                AppRow(
                    displayName = shellProfile.formatAppName(app),
                    onClick = { rowActions.onAppClick(app) },
                    onLongClick = { rowActions.onAppLongClick(app) },
                )
            }
            entry.shortcuts.forEach { shortcut ->
                AppRow(
                    displayName = shellProfile.formatShortcutName(shortcut),
                    onClick = { rowActions.onShortcutClick(shortcut) },
                    onLongClick = { rowActions.onShortcutLongClick(shortcut) },
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

/**
 * One startable line, written in full colour and tall enough to be operated reliably. Pressing it
 * swaps the two terminal colours, the way a TTY marks a selection, so the answer to a touch needs
 * no colour of its own. Holding it writes the command it offers instead of starting it.
 */
@Composable
private fun AppRow(
    displayName: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val colors = LocalTerminalColors.current
    val presses = remember { MutableInteractionSource() }
    val pressed by presses.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(if (pressed) colors.foreground else Color.Transparent)
            .combinedClickable(
                interactionSource = presses,
                indication = null,
                onLongClick = onLongClick,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = displayName,
            style = terminalTextStyle(if (pressed) colors.background else colors.foreground),
        )
    }
}

/** One line Home only writes: the status, what a command printed, and the lines already submitted. */
@Composable
private fun TerminalLine(text: String) {
    val colors = LocalTerminalColors.current
    BasicText(
        text = text,
        style = terminalTextStyle(colors.secondary),
    )
}
