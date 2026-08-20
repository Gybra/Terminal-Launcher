package com.gybra.terminallauncher.launcher

import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import androidx.core.content.IntentCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SystemDeviceLockTest {
    @Test
    fun `reports the launcher unable to lock until the device admin is active`() {
        val context = RuntimeEnvironment.getApplication()
        val deviceLock = SystemDeviceLock(context)

        assertFalse(deviceLock.enabled)

        shadowOf(context.getSystemService(DevicePolicyManager::class.java))
            .setActiveAdmin(adminComponent(context))

        assertTrue(deviceLock.enabled)
    }

    @Test
    fun `locks the screen only while the device admin is active`() {
        val policyManager: DevicePolicyManager = mock()
        val context = contextWith(policyManager)
        whenever(policyManager.isAdminActive(any())).thenReturn(true)

        assertTrue(SystemDeviceLock(context).lock())
        verify(policyManager).lockNow()

        whenever(policyManager.isAdminActive(any())).thenReturn(false)

        assertFalse(SystemDeviceLock(context).lock())
        verify(policyManager, never()).removeActiveAdmin(any())
    }

    @Test
    fun `reports no lock when Android refuses to lock the screen`() {
        val policyManager: DevicePolicyManager = mock()
        whenever(policyManager.isAdminActive(any())).thenReturn(true)
        doThrow(SecurityException("refused")).whenever(policyManager).lockNow()

        assertFalse(SystemDeviceLock(contextWith(policyManager)).lock())
    }

    @Test
    fun `leaves a device without device policy alone`() {
        val context: Context = mock()
        whenever(context.getSystemService(DevicePolicyManager::class.java)).thenReturn(null)
        val deviceLock = SystemDeviceLock(context)

        assertFalse(deviceLock.enabled)
        assertFalse(deviceLock.lock())
        assertFalse(deviceLock.requestEnable())
        deviceLock.disable()
    }

    @Test
    fun `gives the device admin back and keeps an inactive one alone`() {
        val policyManager: DevicePolicyManager = mock()
        val context = contextWith(policyManager)
        whenever(policyManager.isAdminActive(any())).thenReturn(true)

        SystemDeviceLock(context).disable()
        verify(policyManager).removeActiveAdmin(adminComponent(context))

        whenever(policyManager.isAdminActive(any())).thenReturn(false)

        SystemDeviceLock(context).disable()
        verify(policyManager).removeActiveAdmin(any())
    }

    @Test
    fun `asks the user for the device admin`() {
        val context = RuntimeEnvironment.getApplication()

        assertTrue(SystemDeviceLock(context).requestEnable())

        val request = shadowOf(context).nextStartedActivity
        assertEquals(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN, request.action)
        assertEquals(
            adminComponent(context),
            IntentCompat.getParcelableExtra(
                request,
                DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                ComponentName::class.java,
            ),
        )
        assertTrue(
            request.getStringExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION)
                .orEmpty()
                .isNotBlank(),
        )
    }

    @Test
    fun `reports no request on a device that cannot show it`() {
        val policyManager: DevicePolicyManager = mock()
        val context = contextWith(policyManager)
        doThrow(ActivityNotFoundException("none")).whenever(context).startActivity(any())

        assertFalse(SystemDeviceLock(context).requestEnable())
    }

    private fun contextWith(policyManager: DevicePolicyManager): Context {
        val context: Context = mock()
        whenever(context.getSystemService(DevicePolicyManager::class.java)).thenReturn(policyManager)
        whenever(context.packageName).thenReturn("com.gybra.terminallauncher")

        return context
    }

    private fun adminComponent(context: Context): ComponentName =
        ComponentName(context, TerminalDeviceAdminReceiver::class.java)
}
