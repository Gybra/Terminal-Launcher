package com.gybra.terminallauncher.ui.home

import com.gybra.terminallauncher.launcher.AppShortcut
import com.gybra.terminallauncher.launcher.InstalledApp

/**
 * What the prompt can be asked to do. Holding an application offers the commands it can write;
 * choosing one, or holding a shortcut, writes the line. Nothing runs until it is submitted.
 */
public data class PromptActions(
    public val updateValue: (PromptState) -> Unit,
    public val updateFocus: (Boolean) -> Unit,
    public val submit: () -> Unit,
    public val writeShortcutCommand: (AppShortcut) -> Unit,
    public val offerAppCommands: (InstalledApp) -> Unit = {},
    public val writeChoice: (HoldChoice) -> Unit = {},
    public val dismissChoices: () -> Unit = {},
)
