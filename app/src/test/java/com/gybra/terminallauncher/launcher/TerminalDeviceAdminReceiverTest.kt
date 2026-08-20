package com.gybra.terminallauncher.launcher

import android.content.ComponentName
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TerminalDeviceAdminReceiverTest {
    @Test
    fun `names the component Android grants the device admin to`() {
        val context = RuntimeEnvironment.getApplication()

        assertEquals(
            ComponentName(context, TerminalDeviceAdminReceiver::class.java),
            TerminalDeviceAdminReceiver().getWho(context),
        )
    }
}
