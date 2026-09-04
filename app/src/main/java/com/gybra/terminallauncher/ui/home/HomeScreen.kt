package com.gybra.terminallauncher.ui.home

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gybra.terminallauncher.launcher.InstalledApp
import com.gybra.terminallauncher.launcher.AppShortcut
import com.gybra.terminallauncher.shell.SectionLines
import com.gybra.terminallauncher.ui.TestTag
import com.gybra.terminallauncher.ui.terminalTextStyle
import com.gybra.terminallauncher.ui.theme.LocalTerminalColors
import kotlin.math.abs

@Composable
public fun HomeScreen(
    state: HomeUiState,
    onAppClick: (InstalledApp) -> Unit,
    onShortcutClick: (AppShortcut) -> Unit,
    onLockScreen: () -> Unit,
    promptActions: PromptActions,
    modifier: Modifier = Modifier,
    onExpandNotifications: () -> Unit = {},
    onExpandQuickSettings: () -> Unit = {},
    onOpenOverview: () -> Unit = {},
) {
    val colors = LocalTerminalColors.current
    val promptFocus = remember { FocusRequester() }
    BackHandler(enabled = state.holdChoices.isNotEmpty()) {
        promptActions.dismissChoices()
    }
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
            .pointerInput(onExpandNotifications, onExpandQuickSettings, onOpenOverview) {
                detectHomeSwipe(
                    onNotifications = onExpandNotifications,
                    onQuickSettings = onExpandQuickSettings,
                    onOverview = onOpenOverview,
                )
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
                .fillMaxWidth()
                .testTag(TestTag.HOME_LIST.tag),
            verticalArrangement = Arrangement.Bottom,
        ) {
            helpInvitation(line = state.helpInvitation)
            pinnedItems(state = state, rowActions = rowActions)
            terminalHistory(state = state, rowActions = rowActions)
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
 * A vertical swipe that starts in the lower half of Home, including on a row, is a system
 * gesture. Down on the left opens notifications, down on the right opens quick settings, and up
 * opens Overview. The upper half is left to scroll.
 */
private suspend fun PointerInputScope.detectHomeSwipe(
    onNotifications: () -> Unit,
    onQuickSettings: () -> Unit,
    onOverview: () -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(
            requireUnconsumed = false,
            pass = PointerEventPass.Initial,
        )
        val start = down.position
        val end = awaitSwipeEnd(pointerId = down.id, start = start, height = size.height.toFloat())
        if (!isHomeSwipe(start = start, end = end)) {
            return@awaitEachGesture
        }
        val dy = end.y - start.y
        when {
            dy < 0f -> onOverview()
            start.x < size.width / 2f -> onNotifications()
            else -> onQuickSettings()
        }
    }
}

private suspend fun AwaitPointerEventScope.awaitSwipeEnd(
    pointerId: PointerId,
    start: Offset,
    height: Float,
): Offset {
    var current = start
    val slop = viewConfiguration.touchSlop
    val inLowerHalf = start.y >= height / 2f
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        val change = event.changes.firstOrNull { pointer -> pointer.id == pointerId } ?: return current
        current = change.position
        val dy = current.y - start.y
        if (inLowerHalf && abs(dy) > slop && abs(dy) > abs(current.x - start.x)) {
            change.consume()
        }
        if (change.changedToUpIgnoreConsumed()) {
            return current
        }
    }
}

private fun PointerInputScope.isHomeSwipe(start: Offset, end: Offset): Boolean {
    val dy = end.y - start.y
    return start.y >= size.height / 2f &&
        abs(dy) > viewConfiguration.touchSlop &&
        abs(dy) > abs(end.x - start.x)
}

/**
 * How many rows the scrolling region holds, so Home can reach its last one and keep what just
 * happened next to the prompt.
 */
private val HomeUiState.rowCount: Int
    get() = apps.size + shortcuts.size + history.size + searchResults.size +
        (if (helpInvitation == null) 0 else 1) + pinnedSection.rowCount + searchSection.rowCount

/** The lines the shell frames the pinned rows with, and none at all when Home holds none. */
private val HomeUiState.pinnedSection: SectionLines
    get() = if (apps.isEmpty() && shortcuts.isEmpty()) {
        SectionLines()
    } else {
        shellProfile.formatPinnedSection(shellContext, items = apps.size + shortcuts.size)
    }

/** The lines the shell frames the results with, and none at all while the prompt is empty. */
private val HomeUiState.searchSection: SectionLines
    get() = if (prompt.input.isBlank()) {
        SectionLines()
    } else {
        shellProfile.formatSearchSection(matches = searchResults.size)
    }

/** How many rows a section takes, since each side of it is written as one row of lines. */
private val SectionLines.rowCount: Int
    get() = listOf(above, below).count(List<String>::isNotEmpty)

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
 * rows that answer the same two gestures. A tap starts what the row names. A long press on an
 * application offers the commands it can write; choosing one, or holding a shortcut, writes the
 * line and moves to the prompt.
 */
private class RowActions(
    val onAppClick: (InstalledApp) -> Unit,
    val onShortcutClick: (AppShortcut) -> Unit,
    private val promptActions: PromptActions,
    private val promptFocus: FocusRequester,
) {
    fun onAppLongClick(app: InstalledApp, rowKey: String) {
        promptActions.offerAppCommands(app, rowKey)
    }

    fun onChoiceClick(choice: HoldChoice) {
        promptActions.writeChoice(choice)
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
    val section = state.searchSection
    sectionRow(item = HomeItem.SEARCH_HEADER, lines = section.above)
    items(
        items = state.searchResults,
        key = { result -> HomeItem.SEARCH.rowKey(result.app.packageName) },
    ) { result ->
        val rowKey = HomeItem.SEARCH.rowKey(result.app.packageName)
        AppRow(
            displayName = state.shellProfile.formatAppName(result.app),
            onClick = { rowActions.onAppClick(result.app) },
            onLongClick = { rowActions.onAppLongClick(result.app, rowKey) },
            choices = state.choicesUnder(rowKey),
            onChoiceClick = rowActions::onChoiceClick,
        )
    }
    sectionRow(item = HomeItem.SEARCH_FOOTER, lines = section.below)
}

/** Lists what Home keeps above the prompt: the pinned applications, then the pinned shortcuts. */
private fun LazyListScope.pinnedItems(
    state: HomeUiState,
    rowActions: RowActions,
) {
    val section = state.pinnedSection
    sectionRow(item = HomeItem.PINNED_HEADER, lines = section.above)
    items(
        items = state.apps,
        key = InstalledApp::packageName,
    ) { app ->
        AppRow(
            displayName = state.shellProfile.formatAppName(app),
            onClick = { rowActions.onAppClick(app) },
            onLongClick = { rowActions.onAppLongClick(app, app.packageName) },
            choices = state.choicesUnder(app.packageName),
            onChoiceClick = rowActions::onChoiceClick,
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
    sectionRow(item = HomeItem.PINNED_FOOTER, lines = section.below)
}

/** Writes the lines a shell frames a block of rows with, which nothing here can start. */
private fun LazyListScope.sectionRow(item: HomeItem, lines: List<String>) {
    if (lines.isEmpty()) {
        return
    }
    item(key = item.key) {
        Column {
            lines.forEach { line -> TerminalLine(text = line) }
        }
    }
}

/**
 * Writes what every submitted line printed, keeping the applications and shortcuts it listed
 * startable.
 */
private fun LazyListScope.terminalHistory(
    state: HomeUiState,
    rowActions: RowActions,
) {
    items(
        items = state.history,
        key = { entry -> HomeItem.HISTORY.rowKey(entry.id.toString()) },
    ) { entry ->
        Column {
            TerminalLine(text = "${state.shellProfile.prompt(state.shellContext)} ${entry.input}")
            entry.output.forEach { line -> TerminalLine(text = line) }
            entry.apps.forEach { app ->
                val rowKey = HomeItem.HISTORY.rowKey("${entry.id}-${app.packageName}")
                AppRow(
                    displayName = state.shellProfile.formatAppName(app),
                    onClick = { rowActions.onAppClick(app) },
                    onLongClick = { rowActions.onAppLongClick(app, rowKey) },
                    choices = state.choicesUnder(rowKey),
                    onChoiceClick = rowActions::onChoiceClick,
                )
            }
            entry.shortcuts.forEach { shortcut ->
                AppRow(
                    displayName = state.shellProfile.formatShortcutName(shortcut),
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
 * no colour of its own. Holding an application offers the commands it can write instead of
 * starting it, arrested to the output colour so they read as options rather than applications.
 */
@Composable
private fun AppRow(
    displayName: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    choices: List<HoldChoice> = emptyList(),
    onChoiceClick: (HoldChoice) -> Unit = {},
    arrested: Boolean = false,
) {
    val colors = LocalTerminalColors.current
    val ink = if (arrested) colors.secondary else colors.foreground
    val presses = remember { MutableInteractionSource() }
    val pressed by presses.collectIsPressedAsState()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(choices) {
        if (choices.isNotEmpty()) {
            bringIntoViewRequester.bringIntoView()
        }
    }
    Column(modifier = Modifier.bringIntoViewRequester(bringIntoViewRequester)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .background(if (pressed) ink else Color.Transparent)
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
                style = terminalTextStyle(if (pressed) colors.background else ink),
            )
        }
        choices.forEach { choice ->
            AppRow(
                displayName = choice.label,
                onClick = { onChoiceClick(choice) },
                arrested = true,
            )
        }
    }
}

/** The commands offered under [rowKey], or none when another row was held. */
private fun HomeUiState.choicesUnder(rowKey: String): List<HoldChoice> =
    if (holdRowKey == rowKey) holdChoices else emptyList()

/** One line Home only writes: the status, what a command printed, and the lines already submitted. */
@Composable
private fun TerminalLine(text: String) {
    val colors = LocalTerminalColors.current
    BasicText(
        text = text,
        style = terminalTextStyle(colors.secondary),
    )
}
