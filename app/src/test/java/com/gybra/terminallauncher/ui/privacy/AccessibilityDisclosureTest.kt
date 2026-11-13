package com.gybra.terminallauncher.ui.privacy

import androidx.activity.ComponentDialog
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import com.gybra.terminallauncher.theme.TerminalTheme
import com.gybra.terminallauncher.ui.settings.SettingsEntry
import com.gybra.terminallauncher.ui.theme.TerminalThemeProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w320dp-h480dp-port-notnight")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AccessibilityDisclosureTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `long disclosure keeps both explicit choices reachable with large text`() {
        var accepted = 0
        var dismissed = 0
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                TerminalThemeProvider(theme = TerminalTheme.GREEN) {
                    AccessibilityDisclosure(
                        onAccept = { accepted += 1 }, onDismiss = { dismissed += 1 },
                        saving = false, error = null,
                    )
                }
            }
        }
        composeRule.onNodeWithText(AccessibilityDisclosureText.TITLE).assertIsDisplayed()
        val agree = composeRule.onNodeWithText(AccessibilityDisclosureText.AGREE)
        agree.performScrollTo().assertIsDisplayed().assertIsEnabled()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        composeRule.onNodeWithText(AccessibilityDisclosureText.NOT_NOW).performScrollTo().assertIsDisplayed()
        assertEquals(0, accepted)
        assertEquals(0, dismissed)
        agree.performScrollTo().performClick()
        assertEquals(1, accepted)
        assertEquals(0, dismissed)
    }

    @Test
    fun `not now refuses without accepting`() {
        var accepted = false
        var dismissed = false
        composeRule.setContent {
            AccessibilityDisclosure(
                onAccept = { accepted = true }, onDismiss = { dismissed = true },
                saving = false, error = null,
            )
        }
        composeRule.onNodeWithText(AccessibilityDisclosureText.NOT_NOW).performScrollTo().performClick()
        assertEquals(false, accepted)
        assertEquals(true, dismissed)
    }

    @Test
    fun `Android dialog Back is refusal rather than agreement`() {
        var accepted = false
        var dismissed = false
        composeRule.setContent {
            AccessibilityDisclosure(
                onAccept = { accepted = true }, onDismiss = { dismissed = true },
                saving = false, error = null,
            )
        }
        composeRule.runOnIdle {
            (ShadowDialog.getLatestDialog() as ComponentDialog).onBackPressedDispatcher.onBackPressed()
        }
        assertEquals(false, accepted)
        assertEquals(true, dismissed)
    }

    @Test
    fun `saving disables consent controls and Android dialog Back`() {
        var choices = 0
        composeRule.setContent {
            AccessibilityDisclosure(
                onAccept = { choices += 1 }, onDismiss = { choices += 1 }, saving = true, error = null,
            )
        }
        composeRule.onNodeWithText(AccessibilityDisclosureText.AGREE).performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText(AccessibilityDisclosureText.NOT_NOW).performScrollTo().assertIsNotEnabled()
        composeRule.runOnIdle {
            (ShadowDialog.getLatestDialog() as ComponentDialog).onBackPressedDispatcher.onBackPressed()
        }
        assertEquals(0, choices)
    }

    @Test
    fun `a storage error is readable before retrying or declining`() {
        val error = SettingsEntry.STORAGE_ERROR.label
        composeRule.setContent {
            AccessibilityDisclosure(onAccept = {}, onDismiss = {}, saving = false, error = error)
        }
        composeRule.onNodeWithText(error).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(AccessibilityDisclosureText.AGREE).performScrollTo().assertIsEnabled()
        composeRule.onNodeWithText(AccessibilityDisclosureText.NOT_NOW).performScrollTo().assertIsEnabled()
    }
}
