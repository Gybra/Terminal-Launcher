package com.gybra.terminallauncher.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo

/**
 * Reads what an application asks through `ShortcutManager.requestPinShortcut`. Anything Home
 * could not show and start later, such as a widget request or a shortcut without a label, is read
 * as no request at all, so the launcher answers nothing it cannot honor.
 */
public class ShortcutPinRequests(
    private val context: Context,
) {
    public fun read(intent: Intent): ShortcutPinRequest? {
        val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return null
        val request = launcherApps.getPinItemRequest(intent) ?: return null
        if (request.requestType != LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) {
            return null
        }
        val shortcut = request.shortcutInfo?.toAppShortcut() ?: return null

        return AndroidShortcutPinRequest(request = request, shortcut = shortcut)
    }

    private fun ShortcutInfo.toAppShortcut(): AppShortcut? {
        val label = shortLabel?.toString()?.takeIf(String::isNotBlank) ?: return null

        return AppShortcut(packageName = `package`, id = id, label = label)
    }
}

private class AndroidShortcutPinRequest(
    private val request: LauncherApps.PinItemRequest,
    override val shortcut: AppShortcut,
) : ShortcutPinRequest {
    override fun accept(): Boolean = request.accept()
}
