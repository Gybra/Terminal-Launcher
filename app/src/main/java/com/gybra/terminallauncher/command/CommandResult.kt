package com.gybra.terminallauncher.command

/** What the launcher does with submitted prompt input. */
public sealed interface CommandResult {
    /** A registered command ran and produced [lines], which are empty when it only changed state. */
    public data class Output(public val lines: List<String>) : CommandResult

    /** A registered command asked the launcher to erase the terminal history. */
    public object ClearHistory : CommandResult

    /** A registered command asked the launcher to open the settings destination. */
    public object OpenSettings : CommandResult

    /** No registered command matched, so the input is searched among installed applications. */
    public object Search : CommandResult
}
