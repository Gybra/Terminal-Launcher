package com.gybra.terminallauncher.launcher

/** What Android answers when the launcher asks an application for its shortcuts. */
public sealed interface PublishedShortcuts {
    /** The shortcuts the application publishes, which is empty when it publishes none. */
    public data class Available(public val shortcuts: List<AppShortcut>) : PublishedShortcuts

    /** Android refused the question, which it does unless this launcher is the Home application. */
    public object Refused : PublishedShortcuts
}
