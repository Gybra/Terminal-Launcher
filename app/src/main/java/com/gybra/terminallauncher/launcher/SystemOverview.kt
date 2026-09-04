package com.gybra.terminallauncher.launcher

import android.accessibilityservice.AccessibilityService

/**
 * Opens the system Overview through [TerminalAccessibilityService], which asks Android for the
 * same recents switcher the system recents control opens.
 */
public class SystemOverview {
    public fun open(): Boolean = TerminalAccessibilityService.connected
        ?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS) == true
}
