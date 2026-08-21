package com.gybra.terminallauncher.ui.home

import com.gybra.terminallauncher.launcher.AppShortcut
import com.gybra.terminallauncher.launcher.InstalledApp

/**
 * What the prompt can be asked to do. Holding a row writes the command it offers, which is a
 * prompt action rather than a launch: nothing runs until the line is submitted.
 */
public data class PromptActions(
    public val updateValue: (PromptState) -> Unit,
    public val updateFocus: (Boolean) -> Unit,
    public val submit: () -> Unit,
    public val writeAppCommand: (InstalledApp) -> Unit,
    public val writeShortcutCommand: (AppShortcut) -> Unit,
)
