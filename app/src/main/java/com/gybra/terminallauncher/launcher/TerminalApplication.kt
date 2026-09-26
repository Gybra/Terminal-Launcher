package com.gybra.terminallauncher.launcher

import android.app.Application

/** Process-owned notification counts shared by Android's service and the launcher. */
public class TerminalApplication : Application() {
    public val notificationCounts: NotificationCounts = NotificationCounts()
}
