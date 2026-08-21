package com.gybra.terminallauncher.launcher

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process

/**
 * Starts the shortcuts pinned to Home. Android accepts the call only while this launcher is the
 * Home application, so a refused shortcut is reported instead of crashing the prompt.
 */
public class ShortcutLauncher(
    private val context: Context,
) {
    public fun launch(shortcut: AppShortcut): Boolean {
        val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return false

        return try {
            launcherApps.startShortcut(
                shortcut.packageName,
                shortcut.id,
                null,
                null,
                Process.myUserHandle(),
            )
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: IllegalStateException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
