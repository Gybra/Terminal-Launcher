package com.gybra.terminallauncher.launcher

/**
 * A shortcut an application published and the user pinned to Home. [id] identifies the shortcut
 * inside [packageName], and Android needs both of them to start it again.
 */
public data class PinnedShortcut(
    public val packageName: String,
    public val id: String,
    public val label: String,
)
