package com.gybra.terminallauncher.launcher

public interface DeviceLock {
    /** Whether the launcher may lock the screen, which is what the device admin grants. */
    public val enabled: Boolean

    /** Locks the screen, reporting whether Android accepted it. */
    public fun lock(): Boolean

    /** Asks the user for the device admin, reporting whether Android could show the request. */
    public fun requestEnable(): Boolean

    /** Gives the device admin back, so the launcher can no longer lock the screen. */
    public fun disable()
}
