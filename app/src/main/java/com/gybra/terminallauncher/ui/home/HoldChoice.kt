package com.gybra.terminallauncher.ui.home

/**
 * One command a held application offered, labelled as the shell writes it and carrying the line
 * the prompt will receive if it is chosen.
 */
public data class HoldChoice(
    public val label: String,
    public val line: String,
)
