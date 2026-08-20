package com.gybra.terminallauncher.launcher

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argThat
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
    private var launchableApps: List<ResolveInfo> = emptyList()

    @Before
    @Suppress("DEPRECATION")
    fun setUp() {
        packageManager = mock()
        whenever(
            packageManager.queryIntentActivities(
                argThat<Intent> { intent ->
                    intent.action == Intent.ACTION_MAIN &&
                        intent.categories == setOf(Intent.CATEGORY_LAUNCHER)
                },
                eq(PackageManager.MATCH_ALL),
            ),
        ).thenAnswer { launchableApps }
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
                InstalledApp(packageName = "com.example.alpha", label = "Alpha"),
                InstalledApp(packageName = "com.example.zebra", label = "zebra"),
            ),
            repository.getInstalledApps(),
        )
    }

    @Test
    fun `observeInstalledApps emits the current application list`() = runTest {
        stubLaunchableApps(resolveInfo(packageName = "com.example.browser", label = "Browser"))

        assertEquals(
            listOf(InstalledApp(packageName = "com.example.browser", label = "Browser")),
            repository().observeInstalledApps().first(),
        )
    }

    @Test
    fun `observeInstalledApps queries again after every package change`() = runTest {
        val browser = resolveInfo(packageName = "com.example.browser", label = "Browser")
        val mail = resolveInfo(packageName = "com.example.mail", label = "Mail")
        stubLaunchableApps(browser)
        val packageMonitor = FakePackageMonitor()
        val emissions = mutableListOf<List<InstalledApp>>()
        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository(packageMonitor).observeInstalledApps().collect { apps -> emissions += apps }
        }
        runCurrent()

        stubLaunchableApps(browser, mail)
        packageMonitor.report(PackageChange(packageName = "com.example.mail", removed = false))
        runCurrent()

        assertEquals(
            listOf(
                listOf(InstalledApp(packageName = "com.example.browser", label = "Browser")),
                listOf(
                    InstalledApp(packageName = "com.example.browser", label = "Browser"),
                    InstalledApp(packageName = "com.example.mail", label = "Mail"),
                ),
            ),
            emissions,
        )
        collection.cancel()
    }

    private fun repository(packageMonitor: PackageMonitor = FakePackageMonitor()) =
        PackageManagerAppRepository(
            packageManager = packageManager,
            launcherPackageName = OWN_PACKAGE,
            packageMonitor = packageMonitor,
            backgroundDispatcher = Dispatchers.Unconfined,
        )

    private fun stubLaunchableApps(vararg apps: ResolveInfo) {
        launchableApps = apps.toList()
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
