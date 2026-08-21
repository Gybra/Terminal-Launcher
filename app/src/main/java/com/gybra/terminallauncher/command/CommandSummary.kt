package com.gybra.terminallauncher.command

/** The metadata a shell profile needs to describe a registered command in help output. */
public data class CommandSummary(
    public val id: Command,
    public val description: String,
    public val usage: List<String> = emptyList(),
)
