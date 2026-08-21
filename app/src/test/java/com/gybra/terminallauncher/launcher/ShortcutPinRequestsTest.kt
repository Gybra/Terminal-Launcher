package com.gybra.terminallauncher.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Bundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ShortcutPinRequestsTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun `reads the shortcut an application asks to pin`() {
        val request = pinRequest(shortcut = shortcutInfo(id = "new-tab", label = "New tab"))

        val read = ShortcutPinRequests(context).read(intentCarrying(request))

        assertEquals(
            AppShortcut(packageName = context.packageName, id = "new-tab", label = "New tab"),
            read?.shortcut,
        )
    }

    @Test
    fun `accepts the request the intent carries`() {
        val request = pinRequest(shortcut = shortcutInfo(id = "new-tab", label = "New tab"))

        val read = ShortcutPinRequests(context).read(intentCarrying(request))

        assertTrue(read?.accept() == true)
    }

    @Test
    fun `reports a request Android refuses`() {
        val request = pinRequest(
            shortcut = shortcutInfo(id = "new-tab", label = "New tab"),
            accepted = false,
        )

        val read = ShortcutPinRequests(context).read(intentCarrying(request))

        assertEquals(false, read?.accept())
    }

    @Test
    fun `reads no request from an intent that carries none`() {
        assertNull(ShortcutPinRequests(context).read(Intent()))
    }

    @Test
    fun `reads no request for anything other than a shortcut`() {
        val request = pinRequest(
            shortcut = shortcutInfo(id = "new-tab", label = "New tab"),
            requestType = LauncherApps.PinItemRequest.REQUEST_TYPE_APPWIDGET,
        )

        assertNull(ShortcutPinRequests(context).read(intentCarrying(request)))
    }

    @Test
    fun `reads no request for a shortcut without a usable label`() {
        val request = pinRequest(shortcut = shortcutInfo(id = "new-tab", label = " "))

        assertNull(ShortcutPinRequests(context).read(intentCarrying(request)))
    }

    @Test
    fun `reads no request for a shortcut the request does not carry`() {
        assertNull(ShortcutPinRequests(context).read(intentCarrying(pinRequest(shortcut = null))))
    }

    private fun shortcutInfo(id: String, label: String): ShortcutInfo =
        ShortcutInfo.Builder(context, id)
            .setShortLabel(label)
            .setIntent(Intent(Intent.ACTION_VIEW))
            .build()

    /**
     * Builds the request Android would hand a launcher. `PinItemRequest` has no constructor an
     * application can call, so the system side of it is a proxy over the hidden interface.
     */
    private fun pinRequest(
        shortcut: ShortcutInfo?,
        requestType: Int = LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT,
        accepted: Boolean = true,
    ): LauncherApps.PinItemRequest {
        @Suppress("UNCHECKED_CAST")
        val systemType = Class.forName("android.content.pm.IPinItemRequest") as Class<Any>

        return ReflectionHelpers.callConstructor(
            LauncherApps.PinItemRequest::class.java,
            ClassParameter.from(
                systemType,
                ReflectionHelpers.createDelegatingProxy(
                    systemType,
                    SystemPinItemRequest(shortcut = shortcut, accepted = accepted),
                ),
            ),
            ClassParameter.from(Int::class.javaPrimitiveType, requestType),
        )
    }

    private fun intentCarrying(request: LauncherApps.PinItemRequest): Intent =
        Intent().putExtra(LauncherApps.EXTRA_PIN_ITEM_REQUEST, request)

    private class SystemPinItemRequest(
        private val shortcut: ShortcutInfo?,
        private val accepted: Boolean,
    ) {
        fun isValid(): Boolean = true

        fun accept(options: Bundle?): Boolean = accepted

        fun getShortcutInfo(): ShortcutInfo? = shortcut
    }
}
