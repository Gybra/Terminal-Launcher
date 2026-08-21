package com.gybra.terminallauncher.shell

/**
 * The shape a shell writes its prompt cursor as. DOS filled the whole character cell, while a Unix
 * prompt marks the position with the line under it.
 */
public enum class PromptCursor {
    BLOCK,
    UNDERSCORE,
}
