package com.gybra.terminallauncher.command

/** What a [LauncherCommand] receives from the submitted prompt input. */
public data class CommandContext(
    public val arguments: List<String>,
)
