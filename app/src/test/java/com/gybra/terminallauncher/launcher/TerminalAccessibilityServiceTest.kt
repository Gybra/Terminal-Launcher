package com.gybra.terminallauncher.launcher

import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TerminalAccessibilityServiceTest {
    @After
    fun forgetService() {
        TerminalAccessibilityService.connected = null
    }

    @Test
    fun `offers itself while Android keeps it connected`() {
        val service = Robolectric.buildService(TerminalAccessibilityService::class.java).create().get()

        assertNull(TerminalAccessibilityService.connected)

        service.onServiceConnected()

        assertSame(service, TerminalAccessibilityService.connected)

        service.onUnbind(null)

        assertNull(TerminalAccessibilityService.connected)
    }

    @Test
    fun `answers the accessibility stream with nothing`() {
        val service = Robolectric.buildService(TerminalAccessibilityService::class.java).create().get()

        service.onAccessibilityEvent(null)
        service.onInterrupt()

        assertNull(TerminalAccessibilityService.connected)
    }

    @Test
    fun `asks Android for no accessibility events`() {
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }

        val idle = idleAccessibilityInfo(info)

        assertEquals(0, idle.eventTypes)
        assertEquals(0, idle.flags)
        assertSame(info, idle)
    }

    @Test
    fun `builds idle info when Android has none yet`() {
        val idle = idleAccessibilityInfo(null)

        assertEquals(0, idle.eventTypes)
        assertEquals(0, idle.flags)
    }
}
