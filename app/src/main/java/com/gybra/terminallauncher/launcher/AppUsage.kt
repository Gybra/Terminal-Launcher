package com.gybra.terminallauncher.launcher

/**
 * How often and how recently the user launched an application from the prompt. Both values stay
 * at zero for an application that was never launched.
 */
public data class AppUsage(
    public val launchCount: Int = 0,
    public val lastLaunchedAt: Long = 0L,
)
