package com.gybra.terminallauncher.shell

public data class ShellContext(
    public val username: String,
    public val hostname: String,
    public val location: LauncherLocation,
)
