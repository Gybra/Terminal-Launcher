package com.gybra.terminallauncher.launcher

/**
 * A package the system reported as added, changed, or removed. [removed] is true only for a real
 * uninstall, so an application being replaced during an update keeps whatever refers to it.
 */
public data class PackageChange(
    public val packageName: String,
    public val removed: Boolean,
)
