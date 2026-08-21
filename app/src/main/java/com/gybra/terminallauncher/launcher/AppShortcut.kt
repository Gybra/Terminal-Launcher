package com.gybra.terminallauncher.launcher

/**
 * A shortcut an application publishes, whether or not it is pinned to Home. [id] identifies the
 * shortcut inside [packageName], and Android needs both of them to start it.
 */
public data class AppShortcut(
    public val packageName: String,
    public val id: String,
    public val label: String,
)
