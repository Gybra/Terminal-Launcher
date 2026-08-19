package com.gybra.terminallauncher.launcher

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.same
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppLauncherTest {
    private lateinit var context: Context
    private lateinit var packageManager: PackageManager

    private val app = InstalledApp(
        packageName = "com.example.mail",
        label = "Mail",
    )

    @Before
    fun setUp() {
        context = mock()
        packageManager = mock()
        whenever(context.packageManager).thenReturn(packageManager)
    }

    @Test
    fun `launch starts the package launch intent as a new task`() {
        val intent = Intent(Intent.ACTION_MAIN)
        whenever(packageManager.getLaunchIntentForPackage(app.packageName)).thenReturn(intent)

        assertTrue(AppLauncher(context).launch(app))

        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        verify(context).startActivity(same(intent))
    }

    @Test
    fun `launch returns false when the package has no launch intent`() {
        whenever(packageManager.getLaunchIntentForPackage(app.packageName)).thenReturn(null)

        assertFalse(AppLauncher(context).launch(app))
    }

    @Test
    fun `launch returns false when Android cannot find the activity`() {
        stubLaunchIntent()
        doThrow(ActivityNotFoundException()).whenever(context).startActivity(any())

        assertFalse(AppLauncher(context).launch(app))
    }

    @Test
    fun `launch returns false when Android denies the activity start`() {
        stubLaunchIntent()
        doThrow(SecurityException()).whenever(context).startActivity(any())

        assertFalse(AppLauncher(context).launch(app))
    }

    private fun stubLaunchIntent() {
        whenever(packageManager.getLaunchIntentForPackage(app.packageName))
            .thenReturn(Intent(Intent.ACTION_MAIN))
    }
}
