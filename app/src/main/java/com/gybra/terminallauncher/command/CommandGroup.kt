package com.gybra.terminallauncher.command

/**
 * What a command acts on, which is how help gathers the commands it writes. Declaration order is
 * the order help writes the groups in, and every registered command names one, so a command
 * cannot be added without saying where it belongs.
 */
public enum class CommandGroup(public val label: String) {
    APPS("apps"),
    HOME("home"),
    DEVICE("device"),
    LAUNCHER("launcher"),
}
