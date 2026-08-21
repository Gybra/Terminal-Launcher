package com.gybra.terminallauncher.launcher

import android.accessibilityservice.AccessibilityService
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SystemDeviceLockTest {
    @After
    fun disconnectService() {
        TerminalAccessibilityService.connected = null
    }

    @Test
    fun `reports the launcher unable to lock until the service is connected`() {
        val deviceLock = SystemDeviceLock(RuntimeEnvironment.getApplication())

        assertFalse(deviceLock.enabled)

        connectService()

        assertTrue(deviceLock.enabled)
    }

    @Test
    fun `locks the screen the way the power button does`() {
        val service = connectService()

        assertTrue(SystemDeviceLock(RuntimeEnvironment.getApplication()).lock())
        assertEquals(
            listOf(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN),
            shadowOf(service).globalActionsPerformed,
        )
    }

    @Test
    fun `locks nothing while the service is off`() {
        assertFalse(SystemDeviceLock(RuntimeEnvironment.getApplication()).lock())
    }

    @Test
    fun `asks the user for the accessibility service`() {
        val context = RuntimeEnvironment.getApplication()

        assertTrue(SystemDeviceLock(context).requestEnable())

        val request = shadowOf(context).nextStartedActivity
        assertEquals(Settings.ACTION_ACCESSIBILITY_SETTINGS, request.action)
        assertEquals(
            Intent.FLAG_ACTIVITY_NEW_TASK,
            request.flags and Intent.FLAG_ACTIVITY_NEW_TASK,
        )
    }

    @Test
    fun `reports no request on a device that cannot show it`() {
        val context: Context = mock()
        doThrow(ActivityNotFoundException("none")).whenever(context).startActivity(any())

        assertFalse(SystemDeviceLock(context).requestEnable())
    }

    @Test
    fun `turns the service off and leaves an absent one alone`() {
        val service: TerminalAccessibilityService = mock()
        TerminalAccessibilityService.connected = service

        SystemDeviceLock(RuntimeEnvironment.getApplication()).disable()

        verify(service).disableSelf()

        TerminalAccessibilityService.connected = null

        SystemDeviceLock(RuntimeEnvironment.getApplication()).disable()
    }

    private fun connectService(): TerminalAccessibilityService =
        Robolectric.buildService(TerminalAccessibilityService::class.java)
            .create()
            .get()
            .also(TerminalAccessibilityService::onServiceConnected)
}
