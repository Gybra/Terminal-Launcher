package com.gybra.terminallauncher.launcher

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LauncherAppsShortcutRepositoryTest {
    @Test
    fun `reads the shortcuts an application publishes`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val launcherApps: LauncherApps = mock()
        whenever(launcherApps.getShortcuts(any(), any())).thenReturn(
            listOf(
                shortcut(context, id = "new-tab", label = "New tab"),
                shortcut(context, id = "history", label = "History"),
            ),
        )

        val published = repositoryFor(contextWith(launcherApps)).publishedBy("org.example.browser")

        assertEquals(
            PublishedShortcuts.Available(
                listOf(
                    AppShortcut(packageName = "org.example.browser", id = "new-tab", label = "New tab"),
                    AppShortcut(packageName = "org.example.browser", id = "history", label = "History"),
                ),
            ),
            published,
        )
    }

    @Test
    fun `names a shortcut Android publishes without a short label`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val launcherApps: LauncherApps = mock()
        whenever(launcherApps.getShortcuts(any(), any())).thenReturn(
            listOf(
                ShortcutInfo.Builder(context, "compose").setLongLabel("Compose a message").build(),
            ),
        )

        assertEquals(
            PublishedShortcuts.Available(
                listOf(
                    AppShortcut(
                        packageName = "org.example.mail",
                        id = "compose",
                        label = "Compose a message",
                    ),
                ),
            ),
            repositoryFor(contextWith(launcherApps)).publishedBy("org.example.mail"),
        )
    }

    @Test
    fun `reads no shortcut from an application publishing none`() = runTest {
        val launcherApps: LauncherApps = mock()
        whenever(launcherApps.getShortcuts(any(), any())).thenReturn(null)

        assertEquals(
            PublishedShortcuts.Available(emptyList()),
            repositoryFor(contextWith(launcherApps)).publishedBy("org.example.mail"),
        )
    }

    @Test
    fun `reports Android refusing the question`() = runTest {
        val launcherApps: LauncherApps = mock()
        doThrow(SecurityException("not the home application"))
            .whenever(launcherApps)
            .getShortcuts(any(), any())

        assertEquals(
            PublishedShortcuts.Refused,
            repositoryFor(contextWith(launcherApps)).publishedBy("org.example.mail"),
        )
    }

    @Test
    fun `reports Android refusing a question it cannot answer yet`() = runTest {
        val launcherApps: LauncherApps = mock()
        doThrow(IllegalStateException("user is locked"))
            .whenever(launcherApps)
            .getShortcuts(any(), any())

        assertEquals(
            PublishedShortcuts.Refused,
            repositoryFor(contextWith(launcherApps)).publishedBy("org.example.mail"),
        )
    }

    @Test
    fun `leaves a device without launcher applications alone`() = runTest {
        val context: Context = mock()
        whenever(context.getSystemService(LauncherApps::class.java)).thenReturn(null)

        assertEquals(PublishedShortcuts.Refused, repositoryFor(context).publishedBy("org.example.mail"))
    }

    private fun repositoryFor(context: Context): LauncherAppsShortcutRepository =
        LauncherAppsShortcutRepository(context = context, readContext = Dispatchers.Unconfined)

    private fun contextWith(launcherApps: LauncherApps): Context {
        val context: Context = mock()
        whenever(context.getSystemService(LauncherApps::class.java)).thenReturn(launcherApps)

        return context
    }

    private fun shortcut(context: Context, id: String, label: String): ShortcutInfo =
        ShortcutInfo.Builder(context, id)
            .setShortLabel(label)
            .build()
}
