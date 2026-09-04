package com.gybra.terminallauncher.ui.home

import androidx.compose.ui.text.TextRange

public data class PromptState(
    public val input: String = "",
    public val selection: TextRange = TextRange(input.length),
    public val composition: TextRange? = null,
    public val focused: Boolean = false,
    public val generation: Int = 0,
)
