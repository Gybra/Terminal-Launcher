package com.gybra.terminallauncher.ui.home

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PromptTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `tap shows keyboard and Enter submits the current value`() {
        val harness = PromptHarness()
        val keyboard = RecordingKeyboardController()
        setPromptContent(harness, keyboard)

        composeRule.onNodeWithText("user@android:~$", substring = true).performClick()
        composeRule
            .onNodeWithContentDescription("Prompt")
            .assertIsFocused()
            .performTextInput("telegram")

        assertTrue(keyboard.visible)
        composeRule.onNodeWithText("telegram").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Prompt").performImeAction()

        assertEquals(listOf("telegram"), harness.submissions)
        assertEquals("", harness.state.input)
        assertTrue(harness.state.focused)
    }

    @Test
    fun `range selection and visible input stay synchronized while editing`() {
        val harness = PromptHarness()
        setPromptContent(harness, RecordingKeyboardController())
        val prompt = composeRule.onNodeWithContentDescription("Prompt")
        prompt.performClick()
        prompt.performTextInput("telegram")
        prompt.performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.DirectionLeft)
            pressKey(Key.DirectionLeft)
            keyUp(Key.ShiftLeft)
        }

        assertEquals(TextRange(8, 6), harness.state.selection)
        prompt.performTextInput("X")

        assertEquals("telegrX", harness.state.input)
        assertEquals(TextRange(7), harness.state.selection)
        composeRule.onNodeWithText("telegrX").assertIsDisplayed()
    }

    @Test
    fun `text field value round trip preserves selection composition and focus`() {
        val value = TextFieldValue(
            text = "telegram",
            selection = TextRange(2, 5),
            composition = TextRange(0, 8),
        )

        val state = PromptState(focused = true).withTextFieldValue(value)

        assertEquals("telegram", state.input)
        assertEquals(TextRange(2, 5), state.selection)
        assertEquals(TextRange(0, 8), state.composition)
        assertTrue(state.focused)
        assertEquals(value, state.toTextFieldValue())
    }

    @Test
    fun `focused cursor blinks and focus loss leaves it steady`() {
        val harness = PromptHarness()
        composeRule.mainClock.autoAdvance = false
        setPromptContent(harness, RecordingKeyboardController())
        composeRule.onNodeWithContentDescription("Prompt").performClick()

        assertTrue(cursorHasVisiblePixels())
        composeRule.mainClock.advanceTimeBy(500L)
        composeRule.waitForIdle()
        assertTrue(!cursorHasVisiblePixels())
        composeRule.mainClock.advanceTimeBy(500L)
        composeRule.waitForIdle()
        assertTrue(cursorHasVisiblePixels())

        composeRule.runOnIdle { harness.clearFocus() }
        composeRule.waitForIdle()
        assertEquals(false, harness.state.focused)
        composeRule.mainClock.advanceTimeBy(2_000L)
        composeRule.waitForIdle()

        assertTrue(cursorHasVisiblePixels())
    }

    @Test
    fun `back hides keyboard clears focus and preserves input`() {
        val harness = PromptHarness()
        val keyboard = RecordingKeyboardController()
        var backDispatcher: OnBackPressedDispatcher? = null
        composeRule.setContent {
            backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
            CompositionLocalProvider(LocalSoftwareKeyboardController provides keyboard) {
                harness.Content()
            }
        }
        composeRule
            .onNodeWithContentDescription("Prompt")
            .performClick()
            .performTextInput("telegram")
        assertTrue(keyboard.visible)

        composeRule.runOnIdle { checkNotNull(backDispatcher).onBackPressed() }
        composeRule.waitForIdle()

        assertEquals(false, keyboard.visible)
        assertEquals("telegram", harness.state.input)
        assertEquals(false, harness.state.focused)
        composeRule.onNodeWithText("telegram").assertIsDisplayed()
    }

    private fun setPromptContent(
        harness: PromptHarness,
        keyboard: RecordingKeyboardController,
    ) {
        composeRule.setContent {
            CompositionLocalProvider(LocalSoftwareKeyboardController provides keyboard) {
                harness.Content()
            }
        }
    }

    private fun cursorHasVisiblePixels(): Boolean {
        val pixels = composeRule.onNodeWithTag("prompt-input").captureToImage().toPixelMap()
        return (0 until pixels.width).any { x ->
            (0 until pixels.height).any { y -> pixels[x, y] != Color.Black }
        }
    }

    private inner class PromptHarness {
        var state by mutableStateOf(PromptState())
        val submissions = mutableListOf<String>()
        var clearFocus: () -> Unit = {}

        @Composable
        fun Content() {
            androidx.compose.ui.platform.LocalFocusManager.current.let { focusManager ->
                clearFocus = focusManager::clearFocus
            }
            Box(
                modifier = Modifier
                    .background(Color.Black)
                    .testTag("prompt-host"),
            ) {
                Prompt(
                    prompt = "user@android:~$",
                    state = state,
                    actions = PromptActions(
                        updateValue = { value -> state = value },
                        updateFocus = { focused -> state = state.copy(focused = focused) },
                        submit = {
                            submissions += state.input
                            state = state.copy(
                                input = "",
                                selection = TextRange.Zero,
                                composition = null,
                            )
                        },
                    ),
                )
            }
        }
    }

    private class RecordingKeyboardController : SoftwareKeyboardController {
        var visible = false

        override fun show() {
            visible = true
        }

        override fun hide() {
            visible = false
        }
    }
}
