package com.gybra.terminallauncher.launcher

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** Reads metadata only, updating counts on Android events instead of polling. */
public class TerminalNotificationListenerService : NotificationListenerService() {
    private val counts: NotificationCounts
        get() = (application as TerminalApplication).notificationCounts

    override fun onListenerConnected() {
        counts.replace(activeNotifications.orEmpty().map { notification -> notification.toEntry() })
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        counts.post(sbn.toEntry())
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        counts.remove(sbn.key)
    }

    override fun onListenerDisconnected() {
        counts.clear()
    }

    override fun onDestroy() {
        counts.clear()
        super.onDestroy()
    }

    private fun StatusBarNotification.toEntry(): NotificationEntry = NotificationEntry(
        key = key,
        packageName = packageName,
        clearable = isClearable,
        summary = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0,
        group = groupKey,
    )
}
