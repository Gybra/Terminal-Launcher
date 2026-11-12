package com.gybra.terminallauncher.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import com.gybra.terminallauncher.shell.DosDrive
import com.gybra.terminallauncher.shell.PromptSymbol
import com.gybra.terminallauncher.shell.ShellProfile
import com.gybra.terminallauncher.shell.ShellProfiles
import com.gybra.terminallauncher.shell.ShellType
import com.gybra.terminallauncher.shell.dos.DosShellProfile
import com.gybra.terminallauncher.shell.unix.UnixShellProfile
import com.gybra.terminallauncher.theme.BadgeSize
import com.gybra.terminallauncher.theme.DosColor
import com.gybra.terminallauncher.theme.TerminalTheme
import com.gybra.terminallauncher.ui.TestTag
import com.gybra.terminallauncher.ui.badgeDescription
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `starts with every section collapsed under its summary`() {
        val harness = SettingsHarness()
        composeRule.setContent { harness.Content() }

        SettingsSection.entries.forEach { section ->
            header(section).assert(hasOpenState(SettingsEntry.COLLAPSED))
            header(section).assert(hasText(section.summarize(harness.state)))
        }
        composeRule.onAllNodesWithText(label(SettingsEntry.SHELL)).assertCountEquals(0)
    }

    @Test
    fun `opening a section shows its settings and what each one changes`() {
        val harness = SettingsHarness()
        composeRule.setContent { harness.Content() }

        header(SettingsSection.APPEARANCE).performClick()

        header(SettingsSection.APPEARANCE).assert(hasOpenState(SettingsEntry.EXPANDED))
        composeRule.onNodeWithText(label(SettingsEntry.SHELL)).assertIsDisplayed()
        composeRule.onNodeWithText(description(SettingsEntry.SHELL)).assertIsDisplayed()
        composeRule.onNodeWithText(description(SettingsEntry.THEME)).assertIsDisplayed()
    }

    @Test
    fun `opening another section closes the open one, and tapping it again closes it`() {
        val harness = SettingsHarness()
        composeRule.setContent { harness.Content() }

        header(SettingsSection.APPEARANCE).performClick()
        header(SettingsSection.HOME_SCREEN).performClick()

        composeRule.onAllNodesWithText(label(SettingsEntry.SHELL)).assertCountEquals(0)
        composeRule.onNodeWithText(label(SettingsEntry.SHOW_CLOCK)).assertIsDisplayed()

        header(SettingsSection.HOME_SCREEN).performClick()

        composeRule.onAllNodesWithText(label(SettingsEntry.SHOW_CLOCK)).assertCountEquals(0)
        header(SettingsSection.HOME_SCREEN).assert(hasOpenState(SettingsEntry.COLLAPSED))
    }

    @Test
    fun `keeps the open section when the screen is recreated`() {
        val harness = SettingsHarness()
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent { harness.Content() }

        header(SettingsSection.GESTURES).performClick()
        restoration.emulateSavedInstanceStateRestore()

        header(SettingsSection.GESTURES).assert(hasOpenState(SettingsEntry.EXPANDED))
        composeRule.onNodeWithText(label(SettingsEntry.DOUBLE_TAP_TO_LOCK)).assertIsDisplayed()
    }

    @Test
    fun `forwards the shell and every theme`() {
        val harness = SettingsHarness()
        composeRule.setContent { harness.Content() }
        header(SettingsSection.APPEARANCE).performClick()

        tap(SettingsEntry.SHELL.optionKey(ShellType.DOS))
        assertEquals(DosShellProfile, harness.state.shellProfile)
        composeRule.onNodeWithTag(SettingsEntry.SHELL.optionKey(ShellType.DOS)).assertIsSelected()

        TerminalTheme.entries.forEach { theme ->
            tap(SettingsEntry.THEME.optionKey(theme))
            assertEquals(theme, harness.state.terminalTheme)
        }
    }

    @Test
    fun `forwards every home screen toggle and the double tap`() {
        val harness = SettingsHarness()
        composeRule.setContent { harness.Content() }
        header(SettingsSection.HOME_SCREEN).performClick()

        composeRule.onNodeWithText(label(SettingsEntry.SHOW_CLOCK)).assertIsOn().performClick()
        tapRow(SettingsEntry.SHOW_BATTERY)
        tapRow(SettingsEntry.IMMERSIVE_MODE)
        composeRule.onNodeWithText(label(SettingsEntry.SHOW_CLOCK)).assertIsOff()
        header(SettingsSection.GESTURES).performClick()
        tapRow(SettingsEntry.DOUBLE_TAP_TO_LOCK)

        assertFalse(harness.state.showClock)
        assertFalse(harness.state.showBattery)
        assertFalse(harness.state.immersiveMode)
        assertTrue(harness.state.doubleTapToLock)
    }

    @Test
    fun `a toggle announces its meaning and state without reading the drawn checkbox`() {
        val harness = SettingsHarness()
        composeRule.setContent { harness.Content() }
        header(SettingsSection.HOME_SCREEN).performClick()

        composeRule.onNodeWithText(label(SettingsEntry.SHOW_CLOCK))
            .assertIsOn()
            .assertTextEquals(label(SettingsEntry.SHOW_CLOCK), description(SettingsEntry.SHOW_CLOCK))
    }

    @Test
    fun `forwards the unix prompt settings`() {
        val harness = SettingsHarness()
        composeRule.setContent { harness.Content() }
        header(SettingsSection.PROMPT).performClick()

        composeRule.onNodeWithContentDescription(label(SettingsEntry.USERNAME)).performTextReplacement("oreste")
        composeRule.onNodeWithContentDescription(label(SettingsEntry.HOSTNAME)).performTextReplacement("phone")
        listOf(PromptSymbol.PERCENT, PromptSymbol.ARROW).forEach { symbol ->
            tap(SettingsEntry.PROMPT_SYMBOL.optionKey(symbol))
            assertEquals(symbol, harness.state.promptSymbol)
        }
        tapRow(SettingsEntry.SHOW_PROMPT_PATH)

        assertEquals("oreste", harness.state.username)
        assertEquals("phone", harness.state.hostname)
        assertFalse(harness.state.showPromptPath)
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land")
    fun `prompt fields remain editable in the short space left above a landscape keyboard`() {
        val harness = SettingsHarness()
        composeRule.setContent {
            // Leave only the height available above an open landscape keyboard.
            Box(modifier = Modifier.height(180.dp)) { harness.Content() }
        }
        composeRule.onNodeWithTag(TestTag.SETTINGS_LIST.tag).assertHeightIsAtLeast(48.dp)
        header(SettingsSection.PROMPT).performClick()

        scrollTo(hasContentDescription(label(SettingsEntry.USERNAME)))
        composeRule.onNodeWithContentDescription(label(SettingsEntry.USERNAME))
            .assertIsDisplayed().performClick().performTextReplacement("oreste")
        scrollTo(hasContentDescription(label(SettingsEntry.HOSTNAME)))
        composeRule.onNodeWithContentDescription(label(SettingsEntry.HOSTNAME))
            .assertIsDisplayed().performClick().performTextReplacement("phone")

        assertEquals("oreste", harness.state.username)
        assertEquals("phone", harness.state.hostname)
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land")
    fun `a focused hostname stays visible when the landscape viewport shrinks`() {
        val harness = SettingsHarness()
        var availableHeight by mutableStateOf(411.dp)
        composeRule.setContent {
            Box(modifier = Modifier.height(availableHeight)) { harness.Content() }
        }
        header(SettingsSection.PROMPT).performClick()
        scrollTo(hasContentDescription(label(SettingsEntry.HOSTNAME)))
        val hostname = composeRule.onNodeWithContentDescription(label(SettingsEntry.HOSTNAME))
        hostname.performClick().assertIsFocused()

        composeRule.runOnIdle { availableHeight = 180.dp }

        hostname.assertIsFocused().assertIsDisplayed()
        val field = hostname.getUnclippedBoundsInRoot()
        val viewport = composeRule.onNodeWithTag(TestTag.SETTINGS_LIST.tag).getUnclippedBoundsInRoot()
        assertTrue("The focused field must remain above the viewport bottom", field.bottom <= viewport.bottom)
        assertTrue("The focused field must remain below the viewport top", field.top >= viewport.top)
        hostname.performTextReplacement("phone")
        assertEquals("phone", harness.state.hostname)
    }

    @Test
    fun `lists the prompt settings of the active shell and swaps them while open`() {
        val harness = SettingsHarness()
        composeRule.setContent { harness.Content() }
        header(SettingsSection.PROMPT).performClick()
        composeRule.onAllNodesWithText(label(SettingsEntry.DRIVE)).assertCountEquals(0)

        harness.state = harness.state.copy(shellProfile = DosShellProfile)

        composeRule.onAllNodes(hasContentDescription(label(SettingsEntry.USERNAME, DosShellProfile))).assertCountEquals(0)
        listOf(DosDrive.A, DosDrive.D).forEach { drive ->
            tap(SettingsEntry.DRIVE.optionKey(drive))
            assertEquals(drive, harness.state.dosDrive)
        }
        composeRule.onNodeWithText(label(SettingsEntry.SHOW_PROMPT_PATH, DosShellProfile)).assertIsDisplayed()
    }

    @Test
    fun `writes descriptions and summaries in the dos voice`() {
        val harness = SettingsHarness(defaultState().copy(shellProfile = DosShellProfile))
        composeRule.setContent { harness.Content() }

        header(SettingsSection.APPEARANCE).assert(hasText("DOS · SYSTEM"))
        header(SettingsSection.APPEARANCE).performClick()

        composeRule.onNodeWithText(description(SettingsEntry.SHELL, DosShellProfile)).assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsEntry.SHELL.optionKey(ShellType.DOS)).assert(hasText("DOS"))
    }

    @Test
    fun `without notification access offers the android settings and no badge settings`() {
        val harness = SettingsHarness()
        composeRule.setContent { harness.Content() }
        header(SettingsSection.NOTIFICATIONS).performClick()

        composeRule.onNode(accessRow(SettingsEntry.OFF)).assertIsDisplayed()
        composeRule.onAllNodesWithTag(SettingsEntry.BADGE_SIZE.optionKey(BadgeSize.ONE)).assertCountEquals(0)
        tapRow(SettingsEntry.NOTIFICATION_SETTINGS)

        assertTrue(harness.openedNotificationAccess)
    }

    @Test
    fun `granted notification access shows the badge settings instead of the way to grant it`() {
        val harness = SettingsHarness(defaultState().copy(notificationAccess = true))
        composeRule.setContent { harness.Content() }
        header(SettingsSection.NOTIFICATIONS).performClick()

        composeRule.onNode(accessRow(SettingsEntry.NOTIFICATION_ENABLED)).assertIsDisplayed()
        composeRule.onAllNodesWithText(label(SettingsEntry.NOTIFICATION_SETTINGS)).assertCountEquals(0)
        composeRule.onNodeWithTag(SettingsEntry.BADGE_SIZE.optionKey(BadgeSize.TWO)).assertIsSelected()
        tap(SettingsEntry.BADGE_SIZE.optionKey(BadgeSize.THREE))

        assertEquals(BadgeSize.THREE, harness.state.badgeSize)
    }

    @Test
    fun `a badge colour opens the palette, takes a colour, and returns to the theme`() {
        val harness = SettingsHarness(defaultState().copy(notificationAccess = true))
        composeRule.setContent { harness.Content() }
        header(SettingsSection.NOTIFICATIONS).performClick()
        val red = SettingsEntry.BADGE_BACKGROUND.optionKey(DosColor.RED)
        composeRule.onAllNodesWithTag(red).assertCountEquals(0)

        tapRow(SettingsEntry.BADGE_BACKGROUND)
        composeRule.onNode(hasText(label(SettingsEntry.BADGE_BACKGROUND)) and hasOpenState(SettingsEntry.EXPANDED))
            .assertIsDisplayed()
        tap(red)
        assertEquals(DosColor.RED.hex, harness.state.badgeBackground)
        composeRule.onNodeWithTag(red).assertIsSelected()
        header(SettingsSection.NOTIFICATIONS).assert(hasText(SettingsSection.NOTIFICATIONS.summarize(harness.state)))
        tapRow(SettingsEntry.BADGE_RESET)
        assertNull(harness.state.badgeBackground)

        tapRow(SettingsEntry.BADGE_TEXT)
        val white = SettingsEntry.BADGE_TEXT.optionKey(DosColor.WHITE)
        tap(white)
        assertEquals(DosColor.WHITE.hex, harness.state.badgeText)

        tapRow(SettingsEntry.BADGE_BACKGROUND)
        composeRule.onAllNodesWithTag(red).assertCountEquals(0)
        composeRule.onNode(hasText(label(SettingsEntry.BADGE_BACKGROUND)) and hasOpenState(SettingsEntry.COLLAPSED))
            .assertIsDisplayed()
    }

    @Test
    fun `revoking notification access while the section is open swaps the badge settings for the grant action`() {
        val harness = SettingsHarness(defaultState().copy(notificationAccess = true))
        composeRule.setContent { harness.Content() }
        header(SettingsSection.NOTIFICATIONS).performClick()

        harness.state = harness.state.copy(notificationAccess = false)

        header(SettingsSection.NOTIFICATIONS).assert(hasOpenState(SettingsEntry.EXPANDED))
        composeRule.onAllNodesWithTag(SettingsEntry.BADGE_SIZE.optionKey(BadgeSize.TWO)).assertCountEquals(0)
        scrollTo(hasText(label(SettingsEntry.NOTIFICATION_SETTINGS)))
        composeRule.onNodeWithText(label(SettingsEntry.NOTIFICATION_SETTINGS)).assertIsDisplayed()
    }

    @Test
    fun `the preview badge takes the chosen colour`() {
        val harness = SettingsHarness(defaultState().copy(notificationAccess = true))
        composeRule.setContent { harness.Content() }

        harness.state = harness.state.copy(badgeBackground = DosColor.RED.hex)
        composeRule.waitForIdle()

        val badge = composeRule
            .onNode(hasContentDescription(badgeDescription(PreviewSample.NOTIFICATIONS)))
            .fetchSemanticsNode().boundsInRoot
        val inset = with(composeRule.density) { (BadgeSize.TWO.horizontalPadding / 2).toPx() }
        val picture = composeRule.onRoot().captureToImage().asAndroidBitmap()
        assertEquals(
            DosColor.RED.color.toArgb(),
            picture.getPixel((badge.left - inset).toInt(), badge.center.y.toInt()),
        )
    }

    @Test
    fun `the preview writes the status line, the prompt and a badge from the settings`() {
        val harness = SettingsHarness(defaultState().copy(notificationAccess = true))
        composeRule.setContent { harness.Content() }

        inPreview(PreviewSample.CLOCK).assertIsDisplayed()
        inPreview(UnixShellProfile.formatBattery(PreviewSample.BATTERY)).assertIsDisplayed()
        inPreview("user@android:~$").assertIsDisplayed()
        inPreview(UnixShellProfile.formatAppName(PreviewSample.APP)).assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription(badgeDescription(PreviewSample.NOTIFICATIONS))
            .assertIsDisplayed()

        harness.state = harness.state.copy(
            shellProfile = DosShellProfile,
            showClock = false,
            notificationAccess = false,
            badgeSize = BadgeSize.ONE,
        )

        inPreview("C:\\HOME>").assertIsDisplayed()
        inPreview(DosShellProfile.formatAppName(PreviewSample.APP)).assertIsDisplayed()
        composeRule.onAllNodes(previewText(PreviewSample.CLOCK)).assertCountEquals(0)
        composeRule
            .onAllNodes(hasContentDescription(badgeDescription(PreviewSample.NOTIFICATIONS)))
            .assertCountEquals(0)
    }

    @Test
    fun `the preview drops the status line when both of its parts are off`() {
        val harness = SettingsHarness()
        composeRule.setContent { harness.Content() }

        harness.state = harness.state.copy(showClock = false, showBattery = false)

        composeRule.onAllNodes(previewText(UnixShellProfile.formatBattery(PreviewSample.BATTERY))).assertCountEquals(0)
        inPreview("user@android:~$").assertIsDisplayed()
    }

    @Test
    fun `outlines the part of the preview a change touched, only once it changed`() {
        val harness = SettingsHarness()
        composeRule.setContent { harness.Content() }
        assertTrue(promptEdgeBrightness("user@android:~$") < DARK)

        composeRule.mainClock.autoAdvance = false
        harness.state = harness.state.copy(username = "oreste")
        Snapshot.sendApplyNotifications()
        composeRule.mainClock.advanceTimeBy(milliseconds = 100)

        assertTrue(promptEdgeBrightness("oreste@android:~$") > BRIGHT)
    }

    @Test
    fun `changing shell outlines the whole preview whose parts are rewritten`() {
        val harness = SettingsHarness()
        composeRule.setContent { harness.Content() }
        assertTrue(previewEdgeBrightness() < BRIGHT)

        composeRule.mainClock.autoAdvance = false
        harness.state = harness.state.copy(shellProfile = DosShellProfile)
        Snapshot.sendApplyNotifications()
        composeRule.mainClock.advanceTimeBy(milliseconds = 100)

        inPreview(DosShellProfile.formatAppName(PreviewSample.APP)).assertIsDisplayed()
        assertTrue(previewEdgeBrightness() > BRIGHT)
    }

    @Test
    fun `removing only the clock outlines the whole preview and then fades`() {
        val harness = SettingsHarness()
        composeRule.setContent { harness.Content() }
        assertTrue(previewEdgeBrightness() < BRIGHT)

        composeRule.mainClock.autoAdvance = false
        harness.state = harness.state.copy(showClock = false)
        Snapshot.sendApplyNotifications()
        composeRule.mainClock.advanceTimeBy(milliseconds = 100)

        assertTrue(previewEdgeBrightness() > BRIGHT)
        inPreview(UnixShellProfile.formatBattery(PreviewSample.BATTERY)).assertIsDisplayed()
        composeRule.mainClock.advanceTimeBy(milliseconds = 1500)
        assertTrue(previewEdgeBrightness() < BRIGHT)
    }

    @Test
    fun `writes a storage error and forwards the back action`() {
        val harness = SettingsHarness(defaultState().copy(storageError = "Unable to save preferences"))
        composeRule.setContent { harness.Content() }

        composeRule.onNodeWithText("unable to save preferences").assertIsDisplayed()
        composeRule.onNodeWithText(label(SettingsEntry.BACK)).performClick()

        assertTrue(harness.wentBack)
    }

    /**
     * How bright the left edge of the preview prompt is, where its change outline is drawn in the
     * white foreground of the default monochrome colours.
     */
    private fun promptEdgeBrightness(prompt: String): Int {
        val bounds = inPreview(prompt).fetchSemanticsNode().boundsInRoot
        val picture = composeRule.onRoot().captureToImage().asAndroidBitmap()
        return android.graphics.Color.red(picture.getPixel(bounds.left.toInt(), bounds.center.y.toInt()))
    }

    private fun previewEdgeBrightness(): Int {
        val bounds = composeRule.onNodeWithTag(SettingsEntry.PREVIEW.key).fetchSemanticsNode().boundsInRoot
        val inset = with(composeRule.density) { 12.dp.toPx() }
        val picture = composeRule.onRoot().captureToImage().asAndroidBitmap()
        return android.graphics.Color.red(picture.getPixel((bounds.left - inset).toInt(), bounds.center.y.toInt()))
    }

    private fun header(section: SettingsSection): SemanticsNodeInteraction {
        scrollTo(hasTestTag(section.entry.key))
        return composeRule.onNodeWithTag(section.entry.key)
    }

    /** Scrolls to the control tagged [tag] and taps it. */
    private fun tap(tag: String) {
        scrollTo(hasTestTag(tag))
        composeRule.onNodeWithTag(tag).performClick()
    }

    /** Scrolls to the row labelled [entry] and taps it. */
    private fun tapRow(entry: SettingsEntry, profile: ShellProfile = UnixShellProfile) {
        scrollTo(hasText(label(entry, profile)))
        composeRule.onNodeWithText(label(entry, profile)).performClick()
    }

    private fun hasOpenState(entry: SettingsEntry): SemanticsMatcher =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, label(entry))

    private fun scrollTo(matcher: SemanticsMatcher) {
        composeRule.onNodeWithTag(TestTag.SETTINGS_LIST.tag).performScrollToNode(matcher)
    }

    private fun accessRow(status: SettingsEntry): SemanticsMatcher =
        hasText(label(SettingsEntry.NOTIFICATION_ACCESS)) and hasText(label(status))

    private fun previewText(text: String): SemanticsMatcher =
        hasText(text) and hasAnyAncestor(hasTestTag(SettingsEntry.PREVIEW.key))

    private fun inPreview(text: String): SemanticsNodeInteraction = composeRule.onNode(previewText(text))

    /** The label the screen writes for [entry], which the selected shell decides. */
    private fun label(entry: SettingsEntry, profile: ShellProfile = UnixShellProfile): String =
        profile.formatMessage(entry.label)

    private fun description(entry: SettingsEntry, profile: ShellProfile = UnixShellProfile): String =
        profile.formatMessage(entry.description)

    private fun defaultState(): SettingsUiState = SettingsUiState(
        shellProfile = UnixShellProfile,
        terminalTheme = TerminalTheme.SYSTEM,
        showClock = true,
        showBattery = true,
        immersiveMode = true,
        doubleTapToLock = false,
        username = "user",
        hostname = "android",
        promptSymbol = PromptSymbol.DOLLAR,
        showPromptPath = true,
        dosDrive = DosDrive.C,
    )

    private inner class SettingsHarness(
        initialState: SettingsUiState = defaultState(),
    ) {
        var state by mutableStateOf(initialState)
        var wentBack = false
        var openedNotificationAccess = false

        @Composable
        fun Content() {
            SettingsScreen(
                state = state,
                actions = SettingsActions(
                    selectShell = { state = state.copy(shellProfile = ShellProfiles.forType(it)) },
                    selectTheme = { state = state.copy(terminalTheme = it) },
                    setShowClock = { state = state.copy(showClock = it) },
                    setShowBattery = { state = state.copy(showBattery = it) },
                    setImmersiveMode = { state = state.copy(immersiveMode = it) },
                    setDoubleTapToLock = { state = state.copy(doubleTapToLock = it) },
                    setUsername = { state = state.copy(username = it) },
                    setHostname = { state = state.copy(hostname = it) },
                    selectPromptSymbol = { state = state.copy(promptSymbol = it) },
                    setShowPromptPath = { state = state.copy(showPromptPath = it) },
                    selectDosDrive = { state = state.copy(dosDrive = it) },
                    setBadgeBackground = { state = state.copy(badgeBackground = it) },
                    setBadgeText = { state = state.copy(badgeText = it) },
                    setBadgeSize = { state = state.copy(badgeSize = it) },
                    openNotificationAccess = { openedNotificationAccess = true },
                ),
                onBack = { wentBack = true },
            )
        }
    }

    private companion object {
        const val DARK = 64
        const val BRIGHT = 192
    }
}
