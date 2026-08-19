package com.gybra.terminallauncher.preferences

import com.gybra.terminallauncher.shell.ShellType

public data class LauncherPreferences(
    public val shellType: ShellType = ShellType.UNIX,
    public val showClock: Boolean = true,
    public val username: String = "user",
    public val hostname: String = "android",
    public val pinnedPackages: Set<String> = emptySet(),
)
