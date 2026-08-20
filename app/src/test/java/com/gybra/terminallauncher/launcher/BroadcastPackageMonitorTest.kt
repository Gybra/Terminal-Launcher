package com.gybra.terminallauncher.launcher

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Looper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BroadcastPackageMonitorTest {
    private val application: Application = RuntimeEnvironment.getApplication()

    @Test
    fun `reports added, changed, and removed packages`() = runTest {
        val changes = collectChanges()

        broadcast(Intent.ACTION_PACKAGE_ADDED, "com.example.mail")
        broadcast(Intent.ACTION_PACKAGE_CHANGED, "com.example.browser")
        broadcast(Intent.ACTION_PACKAGE_REMOVED, "com.example.camera")
        runCurrent()

        assertEquals(
            listOf(
                PackageChange(packageName = "com.example.mail", removed = false),
                PackageChange(packageName = "com.example.browser", removed = false),
                PackageChange(packageName = "com.example.camera", removed = true),
            ),
            changes,
        )
    }

    @Test
    fun `reports a package being replaced as changed rather than removed`() = runTest {
        val changes = collectChanges()

        broadcast(Intent.ACTION_PACKAGE_REMOVED, "com.example.mail", replacing = true)
        runCurrent()

        assertEquals(
            listOf(PackageChange(packageName = "com.example.mail", removed = false)),
            changes,
        )
    }

    @Test
    fun `ignores broadcasts without a package name`() = runTest {
        val changes = collectChanges()

        application.sendBroadcast(Intent(Intent.ACTION_PACKAGE_ADDED))
        shadowOf(Looper.getMainLooper()).idle()
        runCurrent()

        assertEquals(emptyList<PackageChange>(), changes)
    }

    @Test
    fun `registers the receiver while collected and unregisters it afterwards`() = runTest {
        val registeredBefore = shadowOf(application).registeredReceivers.size

        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            BroadcastPackageMonitor(application).observeChanges().collect {}
        }
        runCurrent()
        assertEquals(registeredBefore + 1, shadowOf(application).registeredReceivers.size)

        collection.cancel()
        runCurrent()

        assertEquals(registeredBefore, shadowOf(application).registeredReceivers.size)
    }

    private fun kotlinx.coroutines.test.TestScope.collectChanges(): List<PackageChange> {
        val changes = mutableListOf<PackageChange>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            BroadcastPackageMonitor(application).observeChanges().collect { change ->
                changes += change
            }
        }
        runCurrent()
        return changes
    }

    private fun broadcast(action: String, packageName: String, replacing: Boolean = false) {
        application.sendBroadcast(
            Intent(action, Uri.fromParts("package", packageName, null))
                .putExtra(Intent.EXTRA_REPLACING, replacing),
        )
        shadowOf(Looper.getMainLooper()).idle()
    }
}
