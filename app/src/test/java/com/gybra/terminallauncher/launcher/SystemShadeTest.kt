package com.gybra.terminallauncher.launcher

import android.accessibilityservice.AccessibilityService
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SystemShadeTest {
    @After
    fun disconnectService() {
        TerminalAccessibilityService.connected = null
    }

    @Test
    fun `expands notifications the way the status bar does`() {
        val service = connectService()

        assertTrue(SystemShade().expandNotifications())
        assertEquals(
            listOf(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS),
            shadowOf(service).globalActionsPerformed,
        )
    }

    @Test
    fun `expands quick settings the way the status bar does`() {
        val service = connectService()

        assertTrue(SystemShade().expandQuickSettings())
        assertEquals(
            listOf(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS),
            shadowOf(service).globalActionsPerformed,
        )
    }

    @Test
    fun `expands nothing while the service is off`() {
        val shade = SystemShade()

        assertFalse(shade.expandNotifications())
        assertFalse(shade.expandQuickSettings())
    }

    private fun connectService(): TerminalAccessibilityService =
        Robolectric.buildService(TerminalAccessibilityService::class.java)
            .create()
            .get()
            .also(TerminalAccessibilityService::onServiceConnected)
}
