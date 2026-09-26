package com.gybra.terminallauncher.launcher

import android.app.Notification
import android.os.Process
import android.service.notification.StatusBarNotification
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowNotificationListenerService
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TerminalNotificationListenerServiceTest {
    @Suppress("DEPRECATION")
    @Test fun `autogroup summary and child count as one notification`() {
        val service = Robolectric.buildService(TerminalNotificationListenerService::class.java).create().get()
        val counts = ApplicationProvider.getApplicationContext<TerminalApplication>().notificationCounts
        counts.clear()
        val summary = Notification.Builder(service, "test")
            .setSmallIcon(android.R.drawable.ic_dialog_info).build().apply {
                flags = flags or Notification.FLAG_GROUP_SUMMARY
            }
        val child = Notification.Builder(service, "test")
            .setSmallIcon(android.R.drawable.ic_dialog_info).build()
        val summarySbn = StatusBarNotification("other.app", "other.app", 1, null, 1000, 0, 0,
            summary, Process.myUserHandle(), 0L).apply { overrideGroupKey = "autogroup" }
        val childSbn = StatusBarNotification("other.app", "other.app", 2, null, 1000, 0, 0,
            child, Process.myUserHandle(), 0L).apply { overrideGroupKey = "autogroup" }
        service.onNotificationPosted(summarySbn)
        service.onNotificationPosted(childSbn)
        assertEquals(mapOf("other.app" to 1), counts.values.value)
        service.onDestroy()
    }

    @Suppress("DEPRECATION")
    @Test fun `posted metadata updates counts and removal and disconnection clear them`() {
        val service = Robolectric.buildService(TerminalNotificationListenerService::class.java).create().get()
        val counts = ApplicationProvider.getApplicationContext<TerminalApplication>().notificationCounts
        val notification = Notification.Builder(service, "test").setContentTitle("private")
            .setSmallIcon(android.R.drawable.ic_dialog_info).build()
        val sbn = StatusBarNotification("other.app", "other.app", 1, null, 1000, 0, 0,
            notification, Process.myUserHandle(), 0L)
        val shadow = Shadows.shadowOf(service) as ShadowNotificationListenerService
        shadow.addActiveNotification(sbn)
        service.onListenerConnected()
        assertEquals(mapOf("other.app" to 1), counts.values.value)
        service.onNotificationPosted(sbn)
        assertEquals(mapOf("other.app" to 1), counts.values.value)
        service.onNotificationRemoved(sbn)
        assertEquals(emptyMap<String, Int>(), counts.values.value)
        service.onNotificationPosted(sbn)
        service.onListenerDisconnected()
        assertEquals(emptyMap<String, Int>(), counts.values.value)
        service.onDestroy()
    }
}
