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
class SystemOverviewTest {
    @After
    fun disconnectService() {
        TerminalAccessibilityService.connected = null
    }

    @Test
    fun `opens Overview the way the recents control does`() {
        val service = connectService()

        assertTrue(SystemOverview().open())
        assertEquals(
            listOf(AccessibilityService.GLOBAL_ACTION_RECENTS),
            shadowOf(service).globalActionsPerformed,
        )
    }

    @Test
    fun `opens nothing while the service is off`() {
        assertFalse(SystemOverview().open())
    }

    private fun connectService(): TerminalAccessibilityService =
        Robolectric.buildService(TerminalAccessibilityService::class.java)
            .create()
            .get()
            .also(TerminalAccessibilityService::onServiceConnected)
}
