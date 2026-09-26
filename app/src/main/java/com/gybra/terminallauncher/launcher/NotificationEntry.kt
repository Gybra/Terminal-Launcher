package com.gybra.terminallauncher.launcher

/** Metadata required to count a notification; never retain its text or extras. */
public data class NotificationEntry(
    public val key: String,
    public val packageName: String,
    public val clearable: Boolean,
    public val summary: Boolean,
    public val group: String?,
)
