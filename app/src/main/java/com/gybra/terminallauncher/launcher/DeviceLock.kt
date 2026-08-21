package com.gybra.terminallauncher.launcher

public interface DeviceLock {
    /** Whether the launcher may lock the screen, which is what the accessibility service grants. */
    public val enabled: Boolean

    /** Locks the screen, reporting whether Android accepted it. */
    public fun lock(): Boolean

    /** Sends the user where the service is turned on, reporting whether Android could show it. */
    public fun requestEnable(): Boolean

    /** Turns the service off, so the launcher can no longer lock the screen. */
    public fun disable()
}
