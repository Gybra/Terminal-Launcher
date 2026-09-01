package com.gybra.terminallauncher.launcher

import android.accessibilityservice.AccessibilityService

/**
 * Opens the notification shade and quick settings through [TerminalAccessibilityService], which
 * asks Android for the same panels the status bar does.
 */
public class SystemShade {
    public fun expandNotifications(): Boolean = TerminalAccessibilityService.connected
        ?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS) == true

    public fun expandQuickSettings(): Boolean = TerminalAccessibilityService.connected
        ?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS) == true
}
