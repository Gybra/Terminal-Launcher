package com.gybra.terminallauncher.ui.home

public data class PromptActions(
    public val updateValue: (PromptState) -> Unit,
    public val updateFocus: (Boolean) -> Unit,
    public val submit: () -> Unit,
)
