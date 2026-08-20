package com.gybra.terminallauncher.command

/** What the launcher does with submitted prompt input. */
public sealed interface CommandResult {
    /** A registered command ran and consumed the input. */
    public object Handled : CommandResult

    /** No registered command matched, so the input is searched among installed applications. */
    public object Search : CommandResult
}
