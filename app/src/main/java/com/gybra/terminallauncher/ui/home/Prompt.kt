package com.gybra.terminallauncher.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.gybra.terminallauncher.shell.PromptCursor
import com.gybra.terminallauncher.ui.TERMINAL_FONT_SIZE
import com.gybra.terminallauncher.ui.TestTag
import com.gybra.terminallauncher.ui.terminalTextStyle
import com.gybra.terminallauncher.ui.theme.LocalTerminalColors

@Composable
internal fun Prompt(
    prompt: String,
    cursor: PromptCursor,
    state: PromptState,
    actions: PromptActions,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val releasePrompt = rememberPromptRelease()
    val keyboardController = LocalSoftwareKeyboardController.current
    BackHandler(enabled = state.focused, onBack = releasePrompt)
    LaunchedEffect(state.generation) {
        if (state.focused) {
            focusRequester.requestFocus()
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable {
                focusRequester.requestFocus()
                keyboardController?.show()
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val colors = LocalTerminalColors.current
        BasicText(text = "$prompt ", style = terminalTextStyle(colors.foreground))
        PromptInput(
            cursor = cursor,
            state = state,
            actions = actions,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
        )
    }
}

/**
 * Hides the keyboard and releases the focus the prompt holds, which is what leaving the typed line
 * alone means: Back does it where it stands, and Home does it when it hands the screen over.
 */
@Composable
internal fun rememberPromptRelease(): () -> Unit {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    return {
        keyboardController?.hide()
        focusManager.clearFocus()
    }
}

@Composable
private fun PromptInput(
    cursor: PromptCursor,
    state: PromptState,
    actions: PromptActions,
    modifier: Modifier,
) {
    val colors = LocalTerminalColors.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val cursorAlpha = if (state.focused) focusedCursorAlpha() else 1f
    key(state.generation) {
    BasicTextField(
        value = state.toTextFieldValue(),
        onValueChange = { value -> submitOrEdit(state = state, value = value, actions = actions) },
        singleLine = false,
        maxLines = 1,
        textStyle = terminalTextStyle(colors.foreground),
        cursorBrush = SolidColor(Color.Transparent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(
            onDone = { actions.submit() },
            onGo = { actions.submit() },
            onSend = { actions.submit() },
            onSearch = { actions.submit() },
        ),
        onTextLayout = { result -> textLayout = result },
        modifier = modifier
            .onPreviewKeyEvent { event ->
                if (event.key != Key.Enter && event.key != Key.NumPadEnter) {
                    return@onPreviewKeyEvent false
                }
                if (event.type == KeyEventType.KeyDown) {
                    actions.submit()
                }
                true
            }
            .horizontalScroll(scrollState)
            .drawWithContent {
                drawContent()
                if (state.selection.collapsed) {
                    textLayout?.getCursorRect(state.selection.end)?.let { position ->
                        drawCursor(
                            shape = cursor,
                            position = position,
                            color = colors.foreground,
                            alpha = cursorAlpha,
                        )
                    }
                }
            }
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    keyboardController?.show()
                }
                if (focusState.isFocused != state.focused) {
                    actions.updateFocus(focusState.isFocused)
                }
            }
            .semantics { contentDescription = "Prompt" }
            .testTag(TestTag.PROMPT_INPUT.tag),
    )
    }
}

internal fun PromptState.toTextFieldValue(): TextFieldValue = TextFieldValue(
    text = input,
    selection = selection,
    composition = composition,
)

internal fun PromptState.withTextFieldValue(value: TextFieldValue): PromptState = copy(
    input = value.text,
    selection = value.selection,
    composition = value.composition,
)

/** SwiftKey and similar IMEs insert a newline instead of an IME action. */
private fun submitOrEdit(
    state: PromptState,
    value: TextFieldValue,
    actions: PromptActions,
) {
    if ('\n' !in value.text && '\r' !in value.text) {
        actions.updateValue(state.withTextFieldValue(value))
        return
    }
    val line = value.text.filter { character -> character != '\n' && character != '\r' }
    actions.updateValue(
        state.copy(input = line, selection = TextRange(line.length), composition = null),
    )
    actions.submit()
}

/** Draws the cursor the way the shell writes it: filling the character cell, or under it. */
private fun DrawScope.drawCursor(
    shape: PromptCursor,
    position: Rect,
    color: Color,
    alpha: Float,
) {
    val cell = TERMINAL_FONT_SIZE.toPx() * MONOSPACE_ADVANCE
    when (shape) {
        PromptCursor.BLOCK -> drawRect(
            color = color,
            topLeft = position.topLeft,
            size = Size(cell, position.height),
            alpha = alpha,
        )
        PromptCursor.UNDERSCORE -> {
            val baseline = position.bottom - CURSOR_BOTTOM_OFFSET.toPx()
            drawLine(
                color = color,
                start = Offset(position.left, baseline),
                end = Offset(position.left + cell, baseline),
                strokeWidth = CURSOR_STROKE_WIDTH.toPx(),
                alpha = alpha,
            )
        }
    }
}

@Composable
private fun focusedCursorAlpha(): Float {
    val transition = rememberInfiniteTransition(label = "prompt cursor")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = CURSOR_BLINK_MILLIS),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "prompt cursor alpha",
    )
    return alpha
}

private const val CURSOR_BLINK_MILLIS = 500

/** A monospace character advances six tenths of its size, which is the cell the cursor covers. */
private const val MONOSPACE_ADVANCE = 0.6f
private val CURSOR_STROKE_WIDTH = 2.dp
private val CURSOR_BOTTOM_OFFSET = 2.dp
