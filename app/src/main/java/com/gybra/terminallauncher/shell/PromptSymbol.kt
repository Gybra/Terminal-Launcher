package com.gybra.terminallauncher.shell

/**
 * The character the Unix prompt ends with. Cosmetic only: none of them grants or implies any
 * privilege, and the DOS profile keeps its own `>` whatever is selected.
 */
public enum class PromptSymbol(public val text: String) {
    DOLLAR("$"),
    PERCENT("%"),
    ARROW(">"),
}
