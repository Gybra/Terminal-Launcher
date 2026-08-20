package com.gybra.terminallauncher.launcher

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ShortcutLauncherTest {
    private lateinit var context: Context
    private lateinit var launcherApps: LauncherApps

    private val shortcut = PinnedShortcut(
        packageName = "org.example.browser",
        id = "new-tab",
        label = "New tab",
    )

    @Before
    fun setUp() {
        context = mock()
        launcherApps = mock()
        whenever(context.getSystemService(LauncherApps::class.java)).thenReturn(launcherApps)
    }

    @Test
    fun `starts a pinned shortcut for the user running the launcher`() {
        assertTrue(ShortcutLauncher(context).launch(shortcut))

        verify(launcherApps).startShortcut(
            eq("org.example.browser"),
            eq("new-tab"),
            isNull(),
            isNull(),
            eq(Process.myUserHandle()),
        )
    }

    @Test
    fun `reports a device without shortcut support`() {
        whenever(context.getSystemService(LauncherApps::class.java)).thenReturn(null)

        assertFalse(ShortcutLauncher(context).launch(shortcut))
    }

    @Test
    fun `reports a shortcut its application no longer publishes`() {
        failStartWith(ActivityNotFoundException())

        assertFalse(ShortcutLauncher(context).launch(shortcut))
    }

    @Test
    fun `reports a shortcut Android refuses to start`() {
        failStartWith(IllegalStateException())

        assertFalse(ShortcutLauncher(context).launch(shortcut))
    }

    @Test
    fun `reports a shortcut the launcher may not read`() {
        failStartWith(SecurityException())

        assertFalse(ShortcutLauncher(context).launch(shortcut))
    }

    private fun failStartWith(failure: Throwable) {
        doThrow(failure)
            .whenever(launcherApps)
            .startShortcut(any<String>(), any(), anyOrNull(), anyOrNull(), any())
    }
}
