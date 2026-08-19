package com.gybra.terminallauncher.launcher

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PackageManagerAppRepositoryTest {
    private lateinit var packageManager: PackageManager

    @Before
    fun setUp() {
        packageManager = mock()
    }

    @Test
    fun `getInstalledApps returns unique launchable apps sorted by label`() = runTest {
        stubLaunchableApps(
            resolveInfo(packageName = "com.example.zebra", label = "zebra"),
            resolveInfo(packageName = "com.example.alpha", label = "Alpha"),
            resolveInfo(packageName = "com.example.alpha", label = "Alpha duplicate"),
            resolveInfo(packageName = OWN_PACKAGE, label = "Terminal Launcher"),
            resolveInfo(packageName = "", label = "Missing package"),
            resolveInfo(packageName = "com.example.blank", label = "   "),
            ResolveInfo().apply { nonLocalizedLabel = "Missing activity info" },
        )
        val repository = repository()

        assertEquals(
            listOf(
                InstalledApp("com.example.alpha", "Alpha"),
                InstalledApp("com.example.zebra", "zebra"),
            ),
            repository.getInstalledApps(),
        )
    }

    @Test
    fun `findApp matches a trimmed label or package name ignoring case`() = runTest {
        stubLaunchableApps(resolveInfo(packageName = "com.example.mail", label = "Mail"))
        val repository = repository()

        assertEquals("com.example.mail", repository.findApp(" MAIL ")?.packageName)
        assertEquals("Mail", repository.findApp("COM.EXAMPLE.MAIL")?.label)
        assertNull(repository.findApp("calendar"))
        assertNull(repository.findApp("   "))
    }

    @Test
    fun `observeInstalledApps emits the current application list`() = runTest {
        stubLaunchableApps(resolveInfo(packageName = "com.example.browser", label = "Browser"))

        assertEquals(
            listOf(InstalledApp("com.example.browser", "Browser")),
            repository().observeInstalledApps().first(),
        )
    }

    private fun repository() = PackageManagerAppRepository(
        packageManager = packageManager,
        launcherPackageName = OWN_PACKAGE,
        backgroundDispatcher = Dispatchers.Unconfined,
    )

    @Suppress("DEPRECATION")
    private fun stubLaunchableApps(vararg apps: ResolveInfo) {
        whenever(
            packageManager.queryIntentActivities(
                any<Intent>(),
                eq(PackageManager.MATCH_ALL),
            ),
        ).thenReturn(apps.toList())
    }

    private fun resolveInfo(packageName: String, label: String) = ResolveInfo().apply {
        activityInfo = ActivityInfo().apply {
            this.packageName = packageName
            name = "$packageName.MainActivity"
        }
        nonLocalizedLabel = label
    }

    private companion object {
        const val OWN_PACKAGE = "com.gybra.terminallauncher"
    }
}
