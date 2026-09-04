package com.gybra.terminallauncher.launcher

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SystemScreenLauncherTest {
    @Test
    fun `opens each fixed destination with its documented action`() {
        assertOpens(SystemScreen.AndroidSettings, Settings.ACTION_SETTINGS)
        assertOpens(SystemScreen.WifiSettings, Settings.ACTION_WIFI_SETTINGS)
        assertOpens(SystemScreen.BluetoothSettings, Settings.ACTION_BLUETOOTH_SETTINGS)
    }

    @Test
    fun `opens the Android details of a package`() {
        val intent = assertOpens(
            screen = SystemScreen.ApplicationDetails("org.example.mail"),
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        )

        assertEquals("package:org.example.mail", intent.data.toString())
    }

    @Test
    fun `asks Android to delete a package instead of removing it`() {
        val intent = assertOpens(
            screen = SystemScreen.Uninstall("org.example.mail"),
            action = Intent.ACTION_DELETE,
        )

        assertEquals("package:org.example.mail", intent.data.toString())
    }

    @Test
    fun `asks Android for permission to request a package delete`() {
        val context = RuntimeEnvironment.getApplication()
        val info = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS,
        )

        assertTrue(
            info.requestedPermissions.orEmpty().asList().contains(
                Manifest.permission.REQUEST_DELETE_PACKAGES,
            ),
        )
    }

    @Test
    fun `keeps working on a device without the destination`() {
        val context: Context = mock()
        doThrow(ActivityNotFoundException()).whenever(context).startActivity(any())

        SystemScreenLauncher(context).open(SystemScreen.BluetoothSettings)
    }

    @Test
    fun `keeps working when Android denies the destination`() {
        val context: Context = mock()
        doThrow(SecurityException()).whenever(context).startActivity(any())

        SystemScreenLauncher(context).open(SystemScreen.WifiSettings)
    }

    private fun assertOpens(screen: SystemScreen, action: String): Intent {
        val context: Context = mock()

        SystemScreenLauncher(context).open(screen)

        val intent = argumentCaptor<Intent>()
        verify(context).startActivity(intent.capture())
        assertEquals(action, intent.firstValue.action)
        assertEquals(
            Intent.FLAG_ACTIVITY_NEW_TASK,
            intent.firstValue.flags and Intent.FLAG_ACTIVITY_NEW_TASK,
        )

        return intent.firstValue
    }
}
