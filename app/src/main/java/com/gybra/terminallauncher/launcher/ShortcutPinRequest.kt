package com.gybra.terminallauncher.launcher

/** A pending request from an application to pin one of its shortcuts to Home. */
public interface ShortcutPinRequest {
    /** The shortcut the application asks the launcher to pin. */
    public val shortcut: AppShortcut

    /** Tells Android the launcher pins [shortcut], reporting whether Android took the answer. */
    public fun accept(): Boolean
}
