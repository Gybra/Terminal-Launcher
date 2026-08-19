package com.gybra.terminallauncher.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LauncherDataStoreTest {
    @Test
    fun `provides one DataStore instance per application context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        assertSame(context.launcherDataStore, context.launcherDataStore)
    }
}
