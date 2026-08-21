package com.gybra.terminallauncher.launcher

import android.accessibilityservice.AccessibilityService
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Locks the screen through [TerminalAccessibilityService], which asks Android for the same lock
 * the power button performs, so fingerprint and face unlock keep working afterwards.
 */
public class SystemDeviceLock(
    private val context: Context,
) : DeviceLock {
    override val enabled: Boolean
        get() = TerminalAccessibilityService.connected != null

    override fun lock(): Boolean = TerminalAccessibilityService.connected
        ?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN) == true

    override fun requestEnable(): Boolean = try {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    override fun disable() {
        TerminalAccessibilityService.connected?.disableSelf()
    }
}
